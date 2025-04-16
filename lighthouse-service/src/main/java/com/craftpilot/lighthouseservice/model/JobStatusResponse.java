package com.craftpilot.lighthouseservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatusResponse {
    private String jobId;
    private boolean complete;
    private String status;
    private String error;
    private Map<String, Object> data;
    private long timestamp;

    // Lombok builder bazen derleyici tarafından tanınmadığında alternatif static builder
    public static JobStatusResponseBuilder builder() {
        return new JobStatusResponseBuilder();
    }

    // Lombok'un generate ettiği builder sınıfını manuel ekleyelim (compile-time hatası durumunda kullanılır)
    public static class JobStatusResponseBuilder {
        private String jobId;
        private boolean complete;
        private String status;
        private String error;
        private Map<String, Object> data;
        private long timestamp;

        public JobStatusResponseBuilder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public JobStatusResponseBuilder complete(boolean complete) {
            this.complete = complete;
            return this;
        }

        public JobStatusResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public JobStatusResponseBuilder error(String error) {
            this.error = error;
            return this;
        }

        public JobStatusResponseBuilder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public JobStatusResponseBuilder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public JobStatusResponse build() {
            return new JobStatusResponse(jobId, complete, status, error, data, timestamp);
        }
    }
}
