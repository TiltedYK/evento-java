-- Run once on MySQL if `size` is missing from `product` (merch filters + admin form).
ALTER TABLE product ADD COLUMN size VARCHAR(64) NULL AFTER barcode;
