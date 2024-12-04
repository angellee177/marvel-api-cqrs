ALTER TABLE cachecharacters
    ADD CONSTRAINT fk_character FOREIGN KEY (character_id) REFERENCES characters(id);
