-- Finds and returns the commonality between various images 
-- That is, if two images share the same credit, this will return that credit
-- otherwise it returns null for the credit.
DROP FUNCTION IF EXISTS public.images_common(INT[]);
CREATE FUNCTION public.images_common(
	ids INT[]
) RETURNS TABLE(
	image_ids INT[],
	credit TEXT,
	taken DATE,
	summary VARCHAR,
	description TEXT,
	geojson VARCHAR,
	tags VARCHAR
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

		SELECT DISTINCT names INTO arr_tags
		FROM (
			SELECT it.image_id,
				STRING_AGG(t.name, ', ') AS names
			FROM image_tag AS it
			JOIN tag AS t
				ON t.tag_id = it.tag_id
			WHERE it.image_id = ANY(ids)
			GROUP BY it.image_id
		) AS q
		LIMIT 2;

		RETURN QUERY SELECT
			ids AS image_ids,
			CASE WHEN ARRAY_LENGTH(arr_credit, 1) = 1 THEN arr_credit[1]
			ELSE NULL END AS credit,
			CASE WHEN ARRAY_LENGTH(arr_taken, 1) = 1 THEN arr_taken[1]
			ELSE NULL END AS taken,
			CASE WHEN ARRAY_LENGTH(arr_summary, 1) = 1 THEN arr_summary[1]
			ELSE NULL END AS summary,
			CASE WHEN ARRAY_LENGTH(arr_description, 1) = 1 THEN arr_description[1]
			ELSE NULL END AS description,
			CASE WHEN ARRAY_LENGTH(arr_geojson, 1) = 1 THEN arr_geojson[1]
			ELSE NULL END AS geojson,
			CASE WHEN ARRAY_LENGTH(arr_tags, 1) = 1 THEN arr_tags[1]
			ELSE NULL END AS tags
		;
	END;
$$ LANGUAGE plpgsql;
