ALTER TABLE course_chat_messages ADD COLUMN message_type VARCHAR(32) NOT NULL DEFAULT 'message';
ALTER TABLE course_chat_messages ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE course_chat_messages ADD COLUMN conference_url VARCHAR(512);
ALTER TABLE course_chat_messages ADD COLUMN conference_active BOOLEAN NOT NULL DEFAULT false;
