ALTER TABLE admission_enquiry
    ALTER COLUMN id TYPE VARCHAR(16)
    USING UPPER(SUBSTRING(MD5(id::text), 1, 16));

ALTER TABLE admission_enquiry
    ADD CONSTRAINT ck_admission_enquiry_id_hex
    CHECK (id ~ '^[0-9A-F]{16}$');

ALTER TABLE admission_enquiry
    ADD CONSTRAINT ck_admission_enquiry_phone_number
    CHECK (phone_number ~ '^[0-9]{10}$') NOT VALID;
