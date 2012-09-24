create table TIPO_ACG
  (DESCRICAO varchar(255) NOT NULL,
   CH_MAX integer NOT NULL,
   PRIMARY KEY (DESCRICAO));

insert into TIPO_ACG values ('I - Participacao em eventos;',75);
insert into TIPO_ACG values ('II - Atuacao em nucleos tematicos;',75);
insert into TIPO_ACG values ('III - Atividades de extensao;',75);
insert into TIPO_ACG values ('IV - Estagio extra-curricular;',210);
insert into TIPO_ACG values ('V - Atividade de iniciacao cientifica;',150);
insert into TIPO_ACG values ('VI - Publicacao de trabalho;',75);
insert into TIPO_ACG values ('VII - Participacao em orgaos colegiados;',75);
insert into TIPO_ACG values ('VIII - Monitoria;',135);
insert into TIPO_ACG values ('IX - Outra atividade a criterio do Colegiado;',75);
insert into TIPO_ACG values ('-',0); --valor para dar certo enquanto TIPO_APROVADO, em ACG, não for setado.


create table ACG
  (DATA_SOLIC varchar(255) NOT NULL,	
   LOGIN varchar(255) NOT NULL,
   MATRICULA varchar(255) NOT NULL,
   NOME varchar(255) NOT NULL,	
   SUBTIPO varchar(255) NOT NULL,
   TIPO varchar(255) NOT NULL,
   DESCRICAO varchar(255) NOT NULL,
   PROFESSOR varchar(255) NOT NULL,	
   DATA_INI varchar(255) NOT NULL,		
   DATA_FIM varchar(255) NOT NULL,		
   CH integer NOT NULL,			
   COMPROVANTE varchar(255) NOT NULL,   
   ESTADO varchar(255) NOT NULL,	
   CH_APROVADA integer NOT NULL,
   TIPO_APROVADO varchar(255) NOT NULL,
   PRIMARY KEY (LOGIN,DATA_SOLIC),
   FOREIGN KEY (TIPO) REFERENCES TIPO_ACG (DESCRICAO),
   FOREIGN KEY (TIPO_APROVADO) REFERENCES TIPO_ACG (DESCRICAO));


create table AVALIACAO_TUTOR
  (DATA_SOLIC varchar(255) NOT NULL,
   LOGIN_ALUNO varchar(255) NOT NULL,
   LOGIN_TUTOR varchar(255) NOT NULL,
   PARECER varchar(255) NOT NULL,
   DECISAO varchar(255) NOT NULL,
   DATA_AVAL varchar(255) NOT NULL,
   PRIMARY KEY (LOGIN_ALUNO,DATA_SOLIC),
   FOREIGN KEY (DATA_SOLIC) REFERENCES ACG (DATA_SOLIC)); 


create table AVALIACAO_RELATOR
  (DATA_SOLIC varchar(255) NOT NULL,
   LOGIN_ALUNO varchar(255) NOT NULL,
   LOGIN_RELATOR varchar(255) NOT NULL,
   PARECER varchar(255) NOT NULL,
   DECISAO varchar(255) NOT NULL,
   DATA_AVAL varchar(255) NOT NULL,
   PRIMARY KEY (LOGIN_ALUNO,DATA_SOLIC),
   FOREIGN KEY (DATA_SOLIC) REFERENCES ACG (DATA_SOLIC));


create table AVALIACAO_COLEGIADO
  (DATA_SOLIC varchar(255) NOT NULL,
   LOGIN_ALUNO varchar(255) NOT NULL,
   N_VOTOS_SIM integer NOT NULL,
   DECISAO varchar(255) NOT NULL,
   DATA_AVAL varchar(255) NOT NULL,
   PRIMARY KEY (LOGIN_ALUNO,DATA_SOLIC),
   FOREIGN KEY (DATA_SOLIC) REFERENCES ACG (DATA_SOLIC));


create table AVALIACAO_SEC_COORD
  (DATA_SOLIC varchar(255) NOT NULL,
   LOGIN_ALUNO varchar(255) NOT NULL,
   DATA_AVAL_SEC varchar(255) NOT NULL,
   DATA_AVAL_COORD varchar(255) NOT NULL,
   PARECER_SEC varchar(255) NOT NULL,
   DECISAO_SEC varchar(255) NOT NULL,
   DECISAO_COORD varchar(255) NOT NULL,
   PRIMARY KEY (LOGIN_ALUNO,DATA_SOLIC),
   FOREIGN KEY (DATA_SOLIC) REFERENCES ACG (DATA_SOLIC));


