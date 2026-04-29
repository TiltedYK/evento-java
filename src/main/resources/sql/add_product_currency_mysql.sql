-- Run once on MySQL to add currency tracking to products.
-- After running this, the admin product form shows a currency picker,
-- and the front-end currency conversion filter will read the stored currency.
ALTER TABLE product ADD COLUMN currency VARCHAR(8) NULL DEFAULT 'TND' AFTER size;
