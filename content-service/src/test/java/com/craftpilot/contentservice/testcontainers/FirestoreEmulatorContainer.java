package com.craftpilot.contentservice.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class FirestoreEmulatorContainer extends GenericContainer<FirestoreEmulatorContainer> {
    private static final int FIRESTORE_PORT = 8080;

    public FirestoreEmulatorContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        withExposedPorts(FIRESTORE_PORT);
        withEnv("FIRESTORE_PROJECT_ID", "test-project");
        withEnv("FIRESTORE_EMULATOR_HOST", "localhost:" + FIRESTORE_PORT);
        withCommand("gcloud", "beta", "emulators", "firestore", "start", "--host-port=0.0.0.0:" + FIRESTORE_PORT);
    }

    public String getEmulatorEndpoint() {
        return getHost() + ":" + getMappedPort(FIRESTORE_PORT);
    }
} 