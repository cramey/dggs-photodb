DROP TABLE tag, image, image_tag;
CREATE TABLE tag (
	tag_id SERIAL PRIMARY KEY,
	name VARCHAR(50)
);

CREATE TABLE project (
	project_id SERIAL PRIMARY KEY,
	name VARCHAR(50)
);

CREATE TABLE image (
	image_id SERIAL PRIMARY KEY,
	project_id INT REFERENCES project(project_id) NULL,
	image BYTEA NOT NULL,
	thumbnail BYTEA NULL,
	filename TEXT NOT NULL,
	taken DATE NULL,
	entered TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
	modified TIMESTAMP WITHOUT TIME ZONE NOT NULL,
	credit TEXT NULL,
	summary VARCHAR(100) NULL,
	description TEXT NULL,
	metadata JSONB NULL,
	search TSVECTOR NOT NULL,
	geog GEOGRAPHY(Point) NULL
);

CREATE TABLE image_tag (
	tag_id INT REFERENCES tag(tag_id) NOT NULL,
	image_id INT REFERENCES image(image_id) NOT NULL,
	PRIMARY KEY(tag_id, image_id)
);


CREATE OR REPLACE FUNCTION image_modified_fn()
RETURNS TRIGGER AS $$
BEGIN
	NEW.modified = NOW();
	NEW.search =
		TO_TSVECTOR('simple', COALESCE(NEW.credit, '')) ||
		TO_TSVECTOR('simple', COALESCE(NEW.filename, '')) ||
		TO_TSVECTOR('simple', COALESCE(NEW.summary, '')) ||
		TO_TSVECTOR('simple', COALESCE(NEW.description, '')) || (
			SELECT TO_TSVECTOR(
				'simple', COALESCE(STRING_AGG(t.name, ' '), '')
			)
			FROM image_tag AS it
			JOIN tag AS t ON t.tag_id = it.tag_id
			WHERE it.image_id = NEW.image_id
		);
	RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

DROP TRIGGER IF EXISTS image_modified_tr ON image;
CREATE TRIGGER image_modified_tr BEFORE INSERT OR UPDATE ON image
FOR EACH ROW EXECUTE PROCEDURE image_modified_fn();


DROP INDEX IF EXISTS image_project_id_idx;
CREATE INDEX image_project_id_idx ON image(project_id);

DROP INDEX IF EXISTS image_taken_idx;
CREATE INDEX image_taken_idx ON image(taken);

DROP INDEX IF EXISTS image_geog_idx;
CREATE INDEX image_geog_idx ON image USING GIST(geog);

DROP INDEX IF EXISTS image_search_idx;
CREATE INDEX image_search_idx ON image USING GIN(search);
