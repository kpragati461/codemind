package com.codemind.domain;

/** Repository ingestion lifecycle, as shown on the dashboard. */
public enum ProjectStatus {
    /** Registered, waiting for the ingestion worker. */
    QUEUED,
    /** Cloning and analysing the repository. */
    INDEXING,
    /** Ingestion finished; fingerprint is available. */
    COMPLETED,
    /** Ingestion failed; see errorMessage. */
    FAILED
}
