CREATE TABLE CacheCharacters (
     "id" UUID PRIMARY KEY NOT NULL,
     "cachekey" TEXT,
     "data" TEXT NOT NULL DEFAULT '[]',
     "createdat" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     "updatedat" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     "expiresat" TIMESTAMP
);
