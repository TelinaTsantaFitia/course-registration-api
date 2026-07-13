CREATE TABLE images (
    id UUID PRIMARY KEY,
    mail VARCHAR(255) NOT NULL,
    nom_fichier VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    s3_key VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
