const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const HOST_UID = "host_alice_123";
const PLAYER_UID = "player_bob_456";
const ROOM_CODE = "K7M9P2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validRoomPayload(hostId = HOST_UID, now = new Date(Date.now() - 2000)) {
  return {
    hostId,
    hostName: "Alice",
    status: "LOBBY",
    difficulty: "EASY",
    questionIds: [1, 2, 3, 4],
    currentQuestionIndex: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function validPlayerPayload(
  userId = PLAYER_UID,
  roomCode = ROOM_CODE,
  now = new Date(Date.now() - 2000)
) {
  return {
    userId,
    roomCode,
    displayName: "Bob",
    score: 0,
    correctCount: 0,
    incorrectCount: 0,
    lastAnsweredIndex: -1,
    lastAnswer: "",
    lastAnswerCorrect: false,
    joinedAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write quiz_rooms or players", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("quiz_rooms").doc(ROOM_CODE).get());
  await assertFails(unauthDb.collection("quiz_rooms").get());
  await assertFails(
    unauthDb.collection("quiz_rooms").doc(ROOM_CODE).set(validRoomPayload())
  );
  await assertFails(
    unauthDb
      .collection("quiz_rooms")
      .doc(ROOM_CODE)
      .collection("players")
      .doc(PLAYER_UID)
      .set(validPlayerPayload())
  );
});

test("Host can create valid room, rejects ghost fields, spoofed hostId, and future timestamps", async () => {
  const hostDb = testEnv.authenticatedContext(HOST_UID).firestore();

  await assertSucceeds(
    hostDb.collection("quiz_rooms").doc(ROOM_CODE).set(validRoomPayload())
  );

  // Reject ghost field / @DocumentId id field
  await assertFails(
    hostDb
      .collection("quiz_rooms")
      .doc("GHOST1")
      .set({ ...validRoomPayload(), id: "GHOST1" })
  );

  // Reject spoofed hostId
  await assertFails(
    hostDb
      .collection("quiz_rooms")
      .doc("SPOOF1")
      .set(validRoomPayload(PLAYER_UID))
  );

  // Reject future timestamp
  await assertFails(
    hostDb
      .collection("quiz_rooms")
      .doc("FUTR01")
      .set(validRoomPayload(HOST_UID, new Date(Date.now() + 60000)))
  );
});

test("Full Kahoot lifecycle: create room, join in LOBBY, play QUESTION, REVEAL, FINISH, terminal lock", async () => {
  const hostDb = testEnv.authenticatedContext(HOST_UID).firestore();
  const playerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const roomRef = hostDb.collection("quiz_rooms").doc(ROOM_CODE);
  const playerDocRef = playerDb
    .collection("quiz_rooms")
    .doc(ROOM_CODE)
    .collection("players")
    .doc(PLAYER_UID);

  // 1. Host creates room
  await assertSucceeds(roomRef.set(validRoomPayload()));

  // 2. Player joins in LOBBY
  await assertSucceeds(playerDocRef.set(validPlayerPayload()));

  // Player cannot spoof another userId or start with score > 0
  await assertFails(
    playerDb
      .collection("quiz_rooms")
      .doc(ROOM_CODE)
      .collection("players")
      .doc(HOST_UID)
      .set(validPlayerPayload(HOST_UID))
  );
  await assertFails(
    hostDb
      .collection("quiz_rooms")
      .doc(ROOM_CODE)
      .collection("players")
      .doc(HOST_UID)
      .set({ ...validPlayerPayload(HOST_UID), score: 500 })
  );

  // 3. Non-host cannot start quiz
  await assertFails(
    playerDb.collection("quiz_rooms").doc(ROOM_CODE).update({
      status: "QUESTION",
      currentQuestionIndex: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // 4. Host starts first question
  await assertSucceeds(
    roomRef.update({
      status: "QUESTION",
      currentQuestionIndex: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Late joiner cannot create player doc once status is QUESTION
  await assertFails(
    hostDb
      .collection("quiz_rooms")
      .doc(ROOM_CODE)
      .collection("players")
      .doc(HOST_UID)
      .set(validPlayerPayload(HOST_UID))
  );

  // 5. Player submits answer during QUESTION
  await assertSucceeds(
    playerDocRef.update({
      score: 100,
      correctCount: 1,
      incorrectCount: 0,
      lastAnsweredIndex: 0,
      lastAnswer: "can",
      lastAnswerCorrect: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Player cannot re-answer the same question index
  await assertFails(
    playerDocRef.update({
      score: 200,
      correctCount: 2,
      incorrectCount: 0,
      lastAnsweredIndex: 0,
      lastAnswer: "can",
      lastAnswerCorrect: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // 6. Host reveals answer
  await assertSucceeds(
    roomRef.update({
      status: "REVEAL",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // 7. Host finishes quiz
  await assertSucceeds(
    roomRef.update({
      status: "FINISHED",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // 8. Terminal state lock: no updates allowed after FINISHED
  await assertFails(
    roomRef.update({
      status: "QUESTION",
      currentQuestionIndex: 1,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Client query alignment: scoped queries succeed, unfiltered list queries fail", async () => {
  const hostDb = testEnv.authenticatedContext(HOST_UID).firestore();
  const playerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();

  await assertSucceeds(
    hostDb.collection("quiz_rooms").doc(ROOM_CODE).set(validRoomPayload())
  );
  await assertSucceeds(
    playerDb
      .collection("quiz_rooms")
      .doc(ROOM_CODE)
      .collection("players")
      .doc(PLAYER_UID)
      .set(validPlayerPayload())
  );

  // Scoped room list by hostId succeeds; unfiltered fails
  await assertSucceeds(
    hostDb.collection("quiz_rooms").where("hostId", "==", HOST_UID).get()
  );
  await assertFails(hostDb.collection("quiz_rooms").get());

  // Scoped players list by roomCode succeeds; unfiltered fails
  await assertSucceeds(
    playerDb
      .collection("quiz_rooms")
      .doc(ROOM_CODE)
      .collection("players")
      .where("roomCode", "==", ROOM_CODE)
      .get()
  );
  await assertFails(
    playerDb
      .collection("quiz_rooms")
      .doc(ROOM_CODE)
      .collection("players")
      .get()
  );
});
