-- Mobil ilovadan kelgan checkout-token bilan saytda bir martalik login sessiyasi ochilgan vaqt.
-- Token faqat BIR MARTA sessiyaga almashtiriladi (havola qayta ochilsa yoki sizib chiqsa — kirish berilmaydi).
ALTER TABLE checkout_tokens ADD COLUMN IF NOT EXISTS session_issued_at TIMESTAMP;
