const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated users cannot read or write any Muza Glasses collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Guest",
      preferredLanguage: "KK",
      voiceGender: "FEMALE",
      voiceSpeed: "MEDIUM",
      selectedGlassesModel: "RAYBAN_META_GEN_2",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("ai_messages").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("photo_records").get()
  );
});

test("UserProfile: owner create, partial update, and shadow-update rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Aibek Nurlan",
      email: "aibek@example.com",
      preferredLanguage: "KK",
      voiceGender: "FEMALE",
      voiceSpeed: "MEDIUM",
      selectedGlassesModel: "RAYBAN_META_GEN_2",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial preference update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "EN",
      voiceGender: "MALE",
      voiceSpeed: "FAST",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow update with ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Immutable createdAt mutation fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "RU",
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user read & write rejected
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "RU",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("AiChatRecord: owner create/list/delete succeed; cross-user and update fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("ai_messages")
      .doc("msg_1")
      .set({
        userId: ALICE_UID,
        role: "user",
        text: "Алдымда не тұр?",
        language: "KK",
        hasAttachment: true,
        createdAt: now,
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ai_messages").get()
  );

  // Messages are immutable (update denied)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("ai_messages")
      .doc("msg_1")
      .update({ text: "Tampered" })
  );

  // Cross-user denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("ai_messages").get()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("ai_messages")
      .doc("msg_1")
      .delete()
  );
});

test("CloudPhotoRecord: owner create, favorite toggle, description update, list, delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 4000);

  const photoRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("photo_records")
    .doc("photo_1");

  await assertSucceeds(
    photoRef.set({
      userId: ALICE_UID,
      localMediaUri: "content://media/external/images/media/42",
      city: "Алматы",
      dateLabel: "19 қар, 2026",
      description: "Алматы, Абай көшесі көрінісі",
      favorite: false,
      glassesModel: "Ray-Ban Meta Gen 2",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("photo_records").get()
  );

  // Favorite toggle succeeds
  await assertSucceeds(
    photoRef.update({
      favorite: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Description edit succeeds
  await assertSucceeds(
    photoRef.update({
      description: "Жаңартылған сипаттама",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Extra @DocumentId 'id' field rejected on create
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("photo_records")
      .doc("photo_bad")
      .set({
        id: "photo_bad",
        userId: ALICE_UID,
        localMediaUri: "",
        city: "Алматы",
        dateLabel: "19 қар, 2026",
        description: "Test",
        favorite: false,
        glassesModel: "Ray-Ban Meta Gen 2",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Cross-user access denied
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("photo_records")
      .doc("photo_1")
      .get()
  );

  await assertSucceeds(photoRef.delete());
});
