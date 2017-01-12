ALTER TABLE image ADD COLUMN image_md5 VARCHAR(32);
UPDATE image SET image_md5 = md5(image);
ALTER TABLE image ALTER COLUMN image_md5 SET NOT NULL;

DELETE FROM image_tag
WHERE image_id IN (			  
	SELECT image_id
	FROM (			  
		SELECT image_id, ROW_NUMBER() OVER (
			PARTITION BY image_md5 ORDER BY image_id
		) AS rnum
		FROM image
	) AS t
	WHERE t.rnum > 1
);

DELETE FROM image
WHERE image_id IN (			  
	SELECT image_id
	FROM (			  
		SELECT image_id, ROW_NUMBER() OVER (
			PARTITION BY image_md5 ORDER BY image_id
		) AS rnum
		FROM image
	) AS t
	WHERE t.rnum > 1
);

ALTER TABLE image ADD CONSTRAINT image_md5_key UNIQUE (image_md5);

ALTER TABLE image ADD COLUMN geog_accuracy SMALLINT NOT NULL DEFAULT 1;
ALTER TABLE image ADD COLUMN ispublic BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE image DROP COLUMN project_id;

DROP TABLE project;
ALTER TABLE image SET (FILLFACTOR = 70);
