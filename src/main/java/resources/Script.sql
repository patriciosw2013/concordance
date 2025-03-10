select t.abbreviation, v."text" from verse v inner join book b on (b.id = v.book_id)
inner join traduction t on (t.id = v.traduction_id)
where b."name" = 'Juan' and v.chapter = 3 and v.verse = 16 and t.abbreviation = 'TLA'


select a.name, b.chapter, b.verse, regexp_replace(regexp_replace(b.text, E'[\\n\\r]+', ' ', 'g'), E'[*«»—]+', '', 'g')
from book a inner join verse b on (a.id = b.book_id) 
where (a.abbreviation = 'Sal.' or a.name = 'Salmos') 
and b.chapter = 42 and b.verse between 1 and 18

-- concordancia
select b.name, v.chapter, v.verse, trim(v."text") || ' ' || '(' || b.name || ' ' || v.chapter || ':' || v.verse || ' DHH)' 
from verse v inner join book b on (b.id = v.book_id)
where v."text" like '%limosna%'

-- DDL Patristica
CREATE TABLE book (	id INTEGER NOT NULL, name VARCHAR(100), 
parent VARCHAR(100), title varchar(300), autor varchar(100), PRIMARY KEY (id));

CREATE TABLE chapter (id INTEGER NOT NULL, book_id INTEGER, name text, PRIMARY KEY (id), FOREIGN KEY(book_id) REFERENCES book (id));

CREATE TABLE verse (id integer NOT NULL primary key, 
book_id INTEGER, chapter_id integer, verse integer, text TEXT, FOREIGN KEY(book_id) REFERENCES book (id));

CREATE TABLE interlineal (id integer NOT NULL primary key, 
book_id INTEGER, chapter integer, verse integer, strong_id integer, word text, type text, meaning TEXT, 
FOREIGN KEY(book_id) REFERENCES book (id));

CREATE TABLE strong (id integer NOT NULL primary key, 
strong_id INTEGER, language text, word text, def_global text, def TEXT, def_alterna text, 
type text, deriva text, def_rv text, testament_id integer);

alter table strong add column testament_id integer;
update strong set testament_id = 1 where language = 'hebreo';
update strong set testament_id = 2 where language = 'griego';

alter table book add column destination varchar(100);
alter table book add column bookDate varchar(70);
alter table book add column abb text;
alter table verse add column chapter varchar(100);
alter table verse add column chapter_id integer;

CREATE TABLE "autor" (
	"id"	INTEGER,
	"name"	TEXT,
	"longname"	TEXT
);

insert into autor values (13, 'S. Tomás de Aquino', 'Tomas de Aquino');

CREATE TABLE notes (id integer NOT NULL primary key, 
book_id INTEGER, text TEXT, PRIMARY KEY (id), FOREIGN KEY(book_id) REFERENCES book (id));
alter table notes add column chapter varchar(100);

-- notacion de robinson
drop table notation;
CREATE TABLE notation (id INTEGER primary key, code text, name text, description text, type text);
insert into notation values (1,'ADV', 'Adverbio', 'Adverbio o adverbio y partícula combinados, puede ponerse algunos sufijos (-C o -I o -S)', 'tipo');
insert into notation values (2,'CONJ', 'Conjunción', 'Conjunción o partícula conjuntiva', 'tipo');
insert into notation values (3,'COND', 'Condicional', 'Partícula condicional o conjunción, puede tener sufijo -C', 'tipo');
insert into notation values (4,'PRT', 'Partícula', 'Partícula, partícula disyuntiva', 'tipo');
insert into notation values (5,'PREP', 'Preposición', 'Preposición', 'tipo');
insert into notation values (6,'INJ', 'Interjección', 'Interjección', 'tipo');
insert into notation values (7,'ARAM', 'Arameo', 'Palabra transliterada del arameo (indeclinable)', 'tipo');
insert into notation values (8,'HEB', 'Hebreo', 'Palabra transliterada del hebreo (indeclinable)', 'tipo');
insert into notation values (9,'N-PRI', 'Sustantivo propio indeclinable', 'Sustantivo propio indeclinable', 'tipo');
insert into notation values (10,'A-NUI', 'Adjetivo numeral indeclinable', 'Adjetivo numeral indeclinable. Puede tener sufijo -ABB', 'tipo');
insert into notation values (11,'N-LI', 'Sustantivo indeclinable', '(Sustantivo) letra indeclinable', 'tipo');
insert into notation values (12,'N-OI', 'Sustantivo de otro tipo', 'Sustantivo de otro tipo indeclinable', 'tipo');
insert into notation values (13,'N', 'Sustantivo', 'Sustantivo, puede tener sufijo -C', 'tipo');
insert into notation values (14,'A', 'Adjetivo', 'Adjetivo, puede tener sufijos -C, -S, -ATT y -ABB', 'tipo');
insert into notation values (15,'R', 'Pronombre relativo', 'Pronombre relativo, puede tener sufijos -P y -ATT', 'tipo');
insert into notation values (16,'C', 'Pronombre reciproco', 'Pronombre reciproco', 'tipo');
insert into notation values (17,'D', 'Pronombre demostrativo', 'Pronombre demostrativo, puede tener sufijo -C', 'tipo');
insert into notation values (18,'T', 'Artículo definido', 'Artículo definido', 'tipo');
insert into notation values (19,'K', 'Pronombre correlativo', 'Pronombre correlativo', 'tipo');
insert into notation values (20,'I', 'Pronombre interrogativo', 'Pronombre interrogativo', 'tipo');
insert into notation values (21,'X', 'Pronombre indefinido', 'Pronombre indefinido', 'tipo');
insert into notation values (22,'Q', 'Pronombre correlativo o interrogativo', 'Pronombre correlativo o interrogativo', 'tipo');
insert into notation values (23,'F', 'Pronombre reflexivo', 'Pronombre reflexivo agregar persona 1,2,3, e.g F-3DSF', 'tipo');
insert into notation values (24,'S', 'Pronombre posesivo', 'Pronombre posesivo. Agregar persona', 'tipo');
insert into notation values (25,'P', 'Pronombre personal', 'Pronombre personal. Bien tiene un número y no tiene género, o bien no tiene número pero si género.', 'tipo');
insert into notation values (26,'V', 'Verbo', 'Verbo', 'tipo');
insert into notation values (27,'S', 'Superlativo', 'Superlativo (usado sólo con adjetivos y algunos adverbios)', 'sufijo no verbal');
insert into notation values (28,'C', 'Comparativo', 'Comparativo (usado sólo con adjetivos y algunos adverbios)', 'sufijo no verbal');
insert into notation values (29,'ABB', 'Forma abreviada', 'Forma abreviada (usado sólo con varios numerales)', 'sufijo no verbal');
insert into notation values (30,'I', 'Interrogativo', 'Interrogativo', 'sufijo no verbal');
insert into notation values (31,'N', 'Negativa', 'Negativa (usado sólo con partículas como PRT-N)', 'sufijo no verbal');
insert into notation values (32,'-C', 'Forma contraida', 'Forma contraida, o dos palabras mezcladas con crasis', 'sufijo no verbal');
insert into notation values (33,'ATT', 'Atico', 'Forma de griego atico', 'sufijo no verbal');
insert into notation values (34,'P', 'Particula adjunta', 'Partícula adjunta (con pronombre relativo)', 'sufijo no verbal');
insert into notation values (35,'M', 'Significado medio', 'Significado medio', 'sufijo verbal');
insert into notation values (36,'C', 'Forma contraída', 'Forma contraída', 'sufijo verbal');
insert into notation values (37,'T', 'Transitivo', 'Transitivo', 'sufijo verbal');
insert into notation values (38,'A', 'Aeólico', 'Aeólico', 'sufijo verbal');
insert into notation values (39,'ATT', 'Ático', 'Ático', 'sufijo verbal');
insert into notation values (40,'AP', 'Forma apocopada', 'Forma apocopada', 'sufijo verbal');
insert into notation values (41,'IRR', 'Forma irregular o impura', 'Forma irregular o impura', 'sufijo verbal');
insert into notation values (42,'N', 'Nominativo', 'Nominativo', 'caso');
insert into notation values (43,'V', 'Vocativo', 'Vocativo', 'caso');
insert into notation values (44,'G', 'Genitivo', 'Genitivo', 'caso');
insert into notation values (45,'D', 'Dativo', 'Dativo', 'caso');
insert into notation values (46,'A', 'Acusativo', 'Acusativo', 'caso');
insert into notation values (47,'S', 'Singular', 'Singular', 'numero');
insert into notation values (48,'P', 'Plural', 'Plural', 'numero');
insert into notation values (49,'M', 'Masculino', 'Masculino', 'genero');
insert into notation values (50,'F', 'Femenino', 'Femenino', 'genero');
insert into notation values (51,'N', 'Neutro', 'Neutro', 'genero');
insert into notation values (52,'P', 'Presente', 'Presente', 'tiempo');
insert into notation values (53,'I', 'Imperfecto', 'Imperfecto', 'tiempo');
insert into notation values (54,'F', 'Futuro', 'Futuro', 'tiempo');
insert into notation values (55,'2F', 'Segundo futuro', 'Segundo futuro', 'tiempo');
insert into notation values (56,'A', 'Aoristo', 'Aoristo', 'tiempo');
insert into notation values (57,'2A', 'Segundo aoristo', 'Segundo aoristo', 'tiempo');
insert into notation values (58,'R', 'Perfecto', 'Perfecto', 'tiempo');
insert into notation values (59,'2R', 'Segundo perfecto', 'Segundo perfecto', 'tiempo');
insert into notation values (60,'L', 'Pluscoamperfecto', 'Pluscoamperfecto', 'tiempo');
insert into notation values (61,'2L', 'Segundo pluscoamperfecto', 'Segundo pluscoamperfecto', 'tiempo');
insert into notation values (62,'X', 'Sin tiempo', 'Sin tiempo establecido (impertivo adverbial)', 'tiempo');
insert into notation values (63,'A', 'Activa', 'Activa', 'voz');
insert into notation values (64,'M', 'Media', 'Media', 'voz');
insert into notation values (65,'P', 'Pasiva', 'Pasiva', 'voz');
insert into notation values (66,'E', 'Bien media o bien pasiva', 'Bien media o bien pasiva', 'voz');
insert into notation values (67,'D', 'Media deponente', 'Media deponente', 'voz');
insert into notation values (68,'O', 'Pasiva deponente', 'Pasiva deponente', 'voz');
insert into notation values (69,'N', 'Media o pasiva deponente', 'Media o pasiva deponente', 'voz');
insert into notation values (70,'Q', 'Activa impersonal', 'Activa impersonal', 'voz');
insert into notation values (71,'X', 'Sin voz establecida', 'Sin voz establecida', 'voz');
insert into notation values (72,'I', 'Indicativo', 'Indicativo', 'modo');
insert into notation values (73,'S', 'Subjuntivo', 'Subjuntivo', 'modo');
insert into notation values (74,'O', 'Optativo', 'Optativo', 'modo');
insert into notation values (75,'M', 'Imperativo', 'Imperativo', 'modo');
insert into notation values (76,'N', 'Infinitivo', 'Infinitivo', 'modo');
insert into notation values (77,'P', 'Participio', 'Participio', 'modo');
insert into notation values (78,'R', 'Participio en sentido imperativo extra', 'Participio en sentido imperativo extra', 'modo');
insert into notation values (79,'1', 'Primera persona', 'Primera persona', 'persona');
insert into notation values (80,'2', 'Segunda persona', 'Segunda persona', 'persona');
insert into notation values (81,'3', 'Tercera persona', 'Tercera persona', 'persona');


-- limpieza
update verse set text = replace(text, '  ', ' ') where text like '%  %';
update verse set text = rtrim(text) where text like '% '
update verse set text = ltrim(text) where text like ' %'

-- crear capitulos
CREATE TABLE chapter (id INTEGER NOT NULL, book_id INTEGER, name text, PRIMARY KEY (id), FOREIGN KEY(book_id) REFERENCES book (id));

insert into chapter
select row_number() OVER(ORDER BY book_id), book_id, chapter from (select distinct book_id, chapter from verse where chapter is not null);

update verse set chapter_id = (select chapter.id from chapter where chapter.book_id = verse.book_id and chapter.name = verse.chapter) 
where chapter is not null;

-- crear notas
insert into notes (id, book_id, text, chapter) 
select <max notes id> + row_number() OVER(ORDER BY Id), book_id, text, chapter from verse where text like '%**'

select replace(text, '**', '') from notes where text like '%**'

update notes set text = replace(text, '**', '') where text like '%**'
update notes set text = replace(text, '2 C o', '2 Co') where text like '%2 C o %'
update notes set text = replace(text, 'Q o', 'Qo') where text like '%Q o %'
update notes set text = replace(text, 'Z a', 'Za') where text like '%Z a %'
update notes set text = replace(text, 'J r', 'Jr') where text like '%J r %'
update notes set text = replace(text, 'T m', 'Tm') where text like '%T m %'
update notes set text = replace(text, 'S t', 'St') where text like '%S t %'
update notes set text = replace(text, 'P r', 'Pr') where text like '%P r %'
update notes set text = replace(text, 'E z', 'Ez') where text like '%E z %'
update notes set text = replace(text, 'M t', 'Mt') where text like '%M t %'

-- cambio con regex
update verse set verse = CAST(substr(text, 0, instr(text, '.')) as integer) where text REGEXP '^(\d+\.)' and verse is null

-- borrar libro
delete from verse where book_id in (select id from book where parent = 'Agustin de Hipona - Sermon de la montaña.docx')
delete from book where parent = 'Agustin de Hipona - Sermon de la montaña.docx'

delete from verse where book_id in (select id from book where autor in ('Baruc Korman', 'Fabian Liendo', 'Charles Stanley'));
delete from chapter where book_id in (select id from book where autor in ('Baruc Korman', 'Fabian Liendo', 'Charles Stanley'));
delete from book where autor in ('Baruc Korman', 'Fabian Liendo', 'Charles Stanley');

-- quitar parentesis de nombre
update book set name = trim(substr(name, 0, instr(name, '('))) where name REGEXP '[\(\)]' and parent = 'Sermones'

-- quitar 0.
update verse set text = replace(text, '0. ', '') where text like '0.%'
update verse set verse = 0 where text like '%**'

-- quitar capitulo
update verse set chapter = replace(chapter, 'CAPÍTULO ', '') where chapter like 'CAPÍTULO%' 
and book_id in (select id from book where parent in ('Gregorio de Nisa - La gran catequesis.docx', 
'Gregorio Magno - Libros morales.docx'))

update book set name = replace(name, 'LIBRO 0 ', '') where name like 'LIBRO 0 %'
update book set name = 'Prólogo' where name = 'LIBRO Prólogo'

-- quitar doble espacio
update verse set text = replace(text, '  ', ' ') where text like '%  %'
update book set name = replace(name, '  ', ' ') where name like '%  %'
update book set title = replace(title, '  ', ' ') where title like '%  %'

-- quitar capitulo 0
update verse set chapter = replace(chapter, 'CAPÍTULO 0 ', '') where chapter like 'CAPÍTULO 0 %'

select v.book_id, v.id, substr(v.text, 
CASE when instr(lower(v.text), 'siempre') - 30 < 0 THEN instr(lower(v.text),'siempre') ELSE instr(lower(v.text),'siempre') - 30 end, 
CASE WHEN length('siempre') + 70 <= length(v.text) THEN length('siempre') + 70 ELSE length(v.text) end) 
|| ' ' || '(' || b.autor || ' ' || b.name || ' ' || coalesce(v.chapter,'') || ')'
from verse v inner join book b on (v.book_id = b.id) where v.text like '% " + in + "%' order by b.autor, b.parent, b.name

select CASE WHEN length('siempre') + 70 <= length(v.text) THEN length('siempre') + 70 ELSE length(v.text) end) 
|| ' ' || '(' || b.autor || ' ' || b.name || ')'
from verse v inner join book b on (v.book_id = b.id) where v.id=15847

update book set parent = replace(parent, 'D:\Libros\Patristica\', '');
select distinct replace(autor, 'Agustin de Hipona', 'S. Agustín') from book where autor = 'Agustin de Hipona';

-- DDL Bible
CREATE TABLE testament (id INTEGER NOT NULL, name VARCHAR(50), PRIMARY KEY (id));

CREATE TABLE book (	id INTEGER NOT NULL, testament_id INTEGER, name VARCHAR(50), 
abbreviation VARCHAR(5), PRIMARY KEY (id), FOREIGN KEY(testament_id) 
REFERENCES testament (id));

CREATE TABLE traduction (id INTEGER NOT NULL, name VARCHAR(100), 
abbreviation VARCHAR(15), PRIMARY KEY (id));

insert into traduction values (1, 'Reina Valera 1960', 'RVR1960');
insert into traduction values (2, 'Nueva Traducción Viviente', 'NTV');
insert into traduction values (3, 'Nueva Traducción Internacional', 'NVI');
insert into traduction values (4, 'Traducción al lenguaje actual', 'TLA');

insert into testament values (1, 'Antiguo testamento');
insert into testament values (2, 'Nuevo testamento');

drop table verse;
CREATE TABLE verse (id integer NOT NULL primary key, 
book_id INTEGER,  
chapter INTEGER, verse INTEGER, text TEXT, FOREIGN KEY(book_id) REFERENCES book (id));

drop table verse;
CREATE TABLE verse (id serial NOT NULL primary key, id_ori integer, 
book_id INTEGER, traduction_id integer, 
chapter INTEGER, verse INTEGER, text TEXT, FOREIGN KEY(book_id) REFERENCES book (id), 
FOREIGN KEY(traduction_id) REFERENCES traduction (id));

update verse set traduction_id = 3 where traduction_id is null;

CREATE INDEX ix_verse_verse ON verse (verse);
CREATE INDEX ix_verse_trad ON verse (traduction_id);
CREATE INDEX ix_verse_chapter ON verse (chapter);
CREATE INDEX ix_verse_book_id ON verse (book_id);
CREATE INDEX ix_book_abbreviation ON book (abbreviation);
CREATE INDEX ix_book_name ON book (name);