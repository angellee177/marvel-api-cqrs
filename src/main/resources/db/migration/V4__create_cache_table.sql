CREATE TABLE CacheCharacters (
     id UUID PRIMARY KEY NOT NULL,                  -- Unique identifier for the cache entry
     character_id UUID NOT NULL,                    -- Foreign key referencing the characters table
     data JSONB,                                    -- Cached character data in JSON format
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Timestamp when the cache entry was created
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Last updated timestamp
     expires_at TIMESTAMP                         -- Timestamp when the cache entry will expire
);
