drop trigger image_modified_tr on image;
drop function image_modified_fn();
alter table image drop column search;
