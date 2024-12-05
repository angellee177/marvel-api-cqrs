CREATE TABLE CacheCharacters (
     id UUID PRIMARY KEY NOT NULL,                  -- Unique identifier for the cache entry
     cacheKey TEXT,                    -- Unique identifier for cache entry
     data JSONB NOT NULL DEFAULT '[]',                                    -- Cached character data in JSON format
     createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Timestamp when the cache entry was created
     updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Last updated timestamp
     expiresAt TIMESTAMP                         -- Timestamp when the cache entry will expire
);
