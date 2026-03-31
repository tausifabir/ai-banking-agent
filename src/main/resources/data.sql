

CREATE TABLE if not exists loan_scheme (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,

                             name VARCHAR(255),

                             min_amount DOUBLE,
                             max_amount DOUBLE,

                             interest_rate VARCHAR(50),

                             description VARCHAR(1000),

                             target_group VARCHAR(100)
);


delete from loan_scheme;


INSERT INTO loan_scheme (name, min_amount, max_amount, interest_rate, description, target_group)
VALUES ('Personal Loan', 100000, 2000000, NULL, 'No collateral or cash security required', 'General'),

       ('AGAMI Personal Loan', NULL, NULL, NULL, 'Loan for educational or personal needs, repayable in installments',
        'General'),

       ('Home Loan', NULL, 40000000, NULL, 'Housing loan facility up to 40 million BDT', 'General'),

       ('TARA Uddokta SME Loan', 500000, NULL, NULL, 'Special loan for women entrepreneurs', 'Women'),

       ('SME Loan', NULL, NULL, '13.75%', 'Collateral-free SME loan', 'SME');