# Pré-requisitos #

  * BOS-5.7.2, disponível em http://www.bonitasoft.com/products/BPM_downloads/all
  * Cliente SVN (sudo apt-get install subversion)
  * BOS-5.7.2 Tomcat Bundle (opcional, para teste em servidor local)

# Sobre o repositório #

  * A versão de desenvolvimento do processo está em https://bpm-infufsm.googlecode.com/svn/processes/trunk/BOS-5.7.2/studio/workspace/
  * A lista de modificações está em http://code.google.com/p/bpm-infufsm/source/list
  * A última versão testada com usuários finais está em http://code.google.com/p/bpm-infufsm/downloads/list

# Organização do ambiente de desenvolvimento #

  1. Instalar uma cópia do studio (BOS-5.7.2) numa pasta 'test'. Essa cópia servirá apenas para testes e não será compartilhada com os outros desenvolvedores.
  1. Instalar outra cópia do studio (BOS-5.7.2) em outra pasta. Essa cópia será versionada, ou seja, estará conectada ao servidor SVN, e servirá para atualizar o repositório. Os passos para isso são:
    1. Logo após a instalação, fazer checkout para inicializar a cópia de trabalho (ver mais abaixo)
    1. Após o checkout, a rotina de trabalho é:
      1. Fazer update para atualizar a cópia de trabalho
      1. Fazer alterações (que já devem ter sido testadas na instalação de testes)
      1. Fazer commit para enviar as alterações ao repositório
> Obs.: A rotina de trabalho deve ser repetida com frequência (pelo menos uma vez ao dia).


# Inicializando a cópia de trabalho #

Instalar o BOS-5.7.2, p.ex. em ~/tools/BOS-5.7.2, e fazer as seguintes operações:

```
cd ~/tools/BOS-5.7.2
svn --force checkout https://bpm-infufsm.googlecode.com/svn/processes/trunk/BOS-5.7.2/studio --username <coloque aqui seu e-mail de desenvolvedor>
```

Depois do checkout, abra o studio e veja que o processo já pode ser aberto diretamente, sem necessidade de import.

# Rotina de trabalho #

Fazer update:
```
cd ~/tools/BOS-5.7.2/studio
svn up
```

Obs.: Na operação de update é provável que ocorram conflitos.

Fazer commit:
```
cd ~/tools/BOS-5.7.2/studio
svn ci -m "Descreva aqui suas alteracoes" 

Ao fazer o primeiro commit, é solicitada uma senha do GoogleCode.com, a senha pode ser gerada em: https://code.google.com/hosting/settings 
Quando necessário, insira o username escolhido (provavelmente o endereço do gmail) e a senha gerada. Pode-se escolher a opção de salvar a senha, então ela não será solicitada nos próximos commits.
```