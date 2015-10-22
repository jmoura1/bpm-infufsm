## Formulário de solicitação de aproveitamento (aluno) ##

  * Matrícula e nome do aluno preenchidos a partir das informações de cadastro do aluno no sistema (desejável)
  * Dropdown do sub-tipo de atividade deve estar ligado com dropdown da classificação da atividade (obrigatório)
  * Validação: data final não deve ser anterior à data inicial (desejável)
  * Validação: carga horária deve ser maior que zero (obrigatório)
  * Professor responsável deve ser escolhido a partir de um Dropdown (obrigatório)
  * Data da solicitação preenchida automaticamente (por default é a data atual do sistema) (desejável)
  * Colocar texto explicativo no formulário, conforme o tipo de ACG, explicando quais comprovantes devem ser anexados (desejável)
  * Habilitar upload de vários arquivos (imagem ou PDF), um para cada comprovante (obrigatório). Esses arquivos devem ficar acessíveis via uma URL ou botão.


## Formulário de parecer do professor responsável ##

  * Obs.: alguns tipos de ACGs não precisam parecer de professor responsável
  * Dados do formulário:
    * Data preenchida automaticamente (desejável)
    * Decisão (Aprovar? Sim/Não)
    * Parecer (texto)
  * O professor deve enxergar os dados preenchidos na solicitação do aluno (obrigatório)
  * Caso o professor não aceite ou sugira modificações, a solicitação volta para o aluno e termina (o aluno terá que abrir outra solicitação - ver se é viável apenas alterar e recomeçar o processo).
  * Caso a solicitação seja aceita, todos os dados (incluindo parecer do professor) seguem para um membro do colegiado (relator).
  * O relator deve ser "sorteado" entre os membros do colegiado.

## Formulário de parecer do relator ##
  * Dados do formulário:
    * Data preenchida automaticamente (desejável)
    * Decisão (Aprovar? Sim/Não)
    * Parecer (texto)
  * O relator deve enxergar os dados preenchidos na solicitação e no parecer do responsável (se houver)
  * Importante: independente de aprovação ou não, a solicitação segue para o colegiado, ou seja, todos os membros do colegiado recebem a solicitação.

## Formulário parecer do colegiado ##
  * Dados do formulário:
    * Data
    * Decisão (texto)
    * Carga horária aprovada (após decisão final)
    * Classificação aprovada (após decisão final)
  * Os membros do colegiado devem enxergar os dados preenchidos nas etapas anteriores do processo
  * Cada membro do colegiado emite opinião favorável ou não ao parecer do relator. Se houver unanimidade, o processo termina. Caso contrário, deve ser enviado um aviso para a coordenação, para a solicitação ser incluída numa reunião presencial.


## Saídas ##
  * Aluno visualiza decisão final de cada solicitação (obrigatório)
  * Aluno lista processos, com resumo de carga horária aprovada (desejável - ver como fazer)
  * Coordenação lista processos (solicitações) por aluno, com resumos de carga horária (desejável - ver como fazer)

## Outros ##
  * Integração com LDAP:
    * Opção 1: a autenticação do usuário é feita diretamente num servidor LDAP, que conhece nomes de usuários e senhas. Informações sobre grupos/papéis ficam na base LDAP (ou podem ser configuradas apenas no Bonita?)
    * Opção 2: obtém-se uma base LDAP e a partir dela é preenchido automaticamente o cadastro de usuários, criando-se manualmente os papéis/grupos. A autenticação é feita somente no Bonita, sem passar pelo servidor LDAP. Problemas: o cadastro deve ser atualizado periodicamente; senhas em diferentes sistemas vão acabar ficando inconsistentes.