CREATE TABLE Characters (
    id UUID PRIMARY KEY NOT NULL,                  -- Unique identifier for the character
    marvelId VARCHAR NOT NULL,
    name VARCHAR NOT NULL UNIQUE,                         -- Name of the character (e.g., "Spider-Man")
    description TEXT,                              -- Description of the character
    lastModified TIMESTAMP,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Timestamp when the character was created
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- Timestamp when the character was last updated
);
