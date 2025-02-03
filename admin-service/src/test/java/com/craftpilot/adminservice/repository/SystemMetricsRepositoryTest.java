package com.craftpilot.adminservice.repository;

import com.craftpilot.adminservice.model.SystemMetrics;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension; 
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemMetricsRepositoryTest {

    @Mock
    private Firestore firestore;

    @Mock
    private CollectionReference collectionReference;

    @Mock
    private DocumentReference documentReference;

    @Mock
    private ApiFuture<WriteResult> writeResultApiFuture;

    @Mock
    private ApiFuture<DocumentSnapshot> documentSnapshotApiFuture;

    @Mock
    private ApiFuture<QuerySnapshot> querySnapshotApiFuture;

    @Mock
    private DocumentSnapshot documentSnapshot;

    @Mock
    private QuerySnapshot querySnapshot;

    private SystemMetricsRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SystemMetricsRepository(firestore);
    }

    @Test
    void testSave() {
        // Given
        SystemMetrics metrics = SystemMetrics.builder()
                .id("test-id")
                .serviceId("service-1")
                .build();

        when(firestore.collection(anyString())).thenReturn(collectionReference);
        when(collectionReference.document(anyString())).thenReturn(documentReference);
        when(documentReference.set(any())).thenReturn(writeResultApiFuture);

        // Mock ApiFuture behavior
        CompletableFuture<WriteResult> completableFuture = new CompletableFuture<>();
        completableFuture.complete(mock(WriteResult.class));
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(writeResultApiFuture).addListener(any(Runnable.class), any(Executor.class));

        // When & Then
        StepVerifier.create(repository.save(metrics))
                .expectNext(metrics)
                .verifyComplete();
    }

    @Test
    void testFindById() {
        // Given
        SystemMetrics metrics = SystemMetrics.builder()
                .id("test-id")
                .serviceId("service-1")
                .build();

        when(firestore.collection(anyString())).thenReturn(collectionReference);
        when(collectionReference.document(anyString())).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotApiFuture);

        // Mock ApiFuture behavior
        CompletableFuture<DocumentSnapshot> completableFuture = new CompletableFuture<>();
        completableFuture.complete(documentSnapshot);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(documentSnapshotApiFuture).addListener(any(Runnable.class), any(Executor.class));
        when(documentSnapshot.toObject(SystemMetrics.class)).thenReturn(metrics);

        // When & Then
        StepVerifier.create(repository.findById("test-id"))
                .expectNext(metrics)
                .verifyComplete();
    }

    @Test
    void testFindAll() {
        // Given
        SystemMetrics metrics1 = SystemMetrics.builder().id("1").build();
        SystemMetrics metrics2 = SystemMetrics.builder().id("2").build();
        List<QueryDocumentSnapshot> documents = Arrays.asList(mock(QueryDocumentSnapshot.class), mock(QueryDocumentSnapshot.class));

        when(firestore.collection(anyString())).thenReturn(collectionReference);
        when(collectionReference.get()).thenReturn(querySnapshotApiFuture);

        // Mock ApiFuture behavior
        CompletableFuture<QuerySnapshot> completableFuture = new CompletableFuture<>();
        completableFuture.complete(querySnapshot);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(querySnapshotApiFuture).addListener(any(Runnable.class), any(Executor.class));
        when(querySnapshot.getDocuments()).thenReturn(documents);
        when(documents.get(0).toObject(SystemMetrics.class)).thenReturn(metrics1);
        when(documents.get(1).toObject(SystemMetrics.class)).thenReturn(metrics2);

        // When & Then
        StepVerifier.create(repository.findAll())
                .expectNext(metrics1, metrics2)
                .verifyComplete();
    }

    @Test
    void testDeleteById() {
        // Given
        when(firestore.collection(anyString())).thenReturn(collectionReference);
        when(collectionReference.document(anyString())).thenReturn(documentReference);
        when(documentReference.delete()).thenReturn(writeResultApiFuture);

        // Mock ApiFuture behavior
        CompletableFuture<WriteResult> completableFuture = new CompletableFuture<>();
        completableFuture.complete(mock(WriteResult.class));
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(writeResultApiFuture).addListener(any(Runnable.class), any(Executor.class));

        // When & Then
        StepVerifier.create(repository.deleteById("test-id"))
                .verifyComplete();
    }
} 