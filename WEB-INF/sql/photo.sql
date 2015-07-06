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


-- Finds and returns the commonality between various images 
-- That is, if two images share the same credit, this will return that credit
-- otherwise it returns null for the credit.
DROP FUNCTION IF EXISTS public.images_common(INT[]);
CREATE FUNCTION public.images_common(
	ids INT[]
) RETURNS TABLE(
	credit TEXT[],
	taken DATE[],
	summary VARCHAR[],
	description TEXT[],
	geojson TEXT[],
	tags TEXT[]
) AS $$
	DECLARE
		arr_credit TEXT[];
		arr_taken DATE[];
		arr_summary VARCHAR[];
		arr_description TEXT[];
		arr_geojson VARCHAR[];
		arr_tags VARCHAR[];
	BEGIN
		SELECT ARRAY_AGG(q.credit) INTO arr_credit
		FROM (
			SELECT DISTINCT i.credit
			FROM image AS i
			WHERE i.image_id = ANY(ids)
			LIMIT 2
		) AS q;

		SELECT ARRAY_AGG(q.taken) INTO arr_taken
		FROM (
			SELECT DISTINCT i.taken
			FROM image AS i
			WHERE i.image_id = ANY(ids)
			LIMIT 2
		) AS q;

		SELECT ARRAY_AGG(q.summary) INTO arr_summary
		FROM (
			SELECT DISTINCT i.summary
			FROM image AS i
			WHERE i.image_id = ANY(ids)
			LIMIT 2
		) AS q;

		SELECT ARRAY_AGG(q.description) INTO arr_description
		FROM (
			SELECT DISTINCT i.description
			FROM image AS i
			WHERE i.image_id = ANY(ids)
			LIMIT 2
		) AS q;

		SELECT ARRAY_AGG(q.geojson) INTO arr_geojson
		FROM (
			SELECT DISTINCT
				ST_AsGeoJSON(i.geog::GEOMETRY) AS geojson
			FROM image AS i
			WHERE i.image_id = ANY(ids)
			LIMIT 2
		) AS q;

		SELECT ARRAY_AGG(q.names) INTO arr_tags
		FROM (
			SELECT DISTINCT names
			FROM (
				SELECT i.image_id,
					STRING_AGG(t.name, ', ') AS names
				FROM image AS i
				LEFT OUTER JOIN image_tag AS it
					ON it.image_id = i.image_id
				LEFT OUTER JOIN tag AS t
					ON t.tag_id = it.tag_id
				WHERE i.image_id = ANY(ids)
				GROUP BY i.image_id
			) AS q
			LIMIT 2
		) AS q;

		RETURN QUERY SELECT
			CASE WHEN ARRAY_LENGTH(arr_credit, 1) = 1
				THEN ARRAY[arr_credit[1]]
				ELSE ARRAY[]::TEXT[]
			END AS credit,

			CASE WHEN ARRAY_LENGTH(arr_taken, 1) = 1
				THEN ARRAY[arr_taken[1]]
				ELSE ARRAY[]::DATE[]
			END AS taken,

			CASE WHEN ARRAY_LENGTH(arr_summary, 1) = 1
				THEN ARRAY[arr_summary[1]]
				ELSE ARRAY[]::VARCHAR[]
			END AS summary,

			CASE WHEN ARRAY_LENGTH(arr_description, 1) = 1
				THEN ARRAY[arr_description[1]]
				ELSE ARRAY[]::TEXT[]
			END AS description,

			CASE WHEN ARRAY_LENGTH(arr_geojson, 1) = 1
				THEN ARRAY[arr_geojson[1]]
				ELSE ARRAY[]::TEXT[]
			END AS geojson,

			CASE WHEN ARRAY_LENGTH(arr_tags, 1) = 1
				THEN ARRAY[arr_tags[1]]
				ELSE ARRAY[]::TEXT[]
			END AS tags
		;
	END;
$$ LANGUAGE plpgsql;
