# AGENTS.md — LembreBem | Diretrizes do Codex

Leia `LEMBREBEM_COMANDO_MESTRE_CODEX.md` e siga integralmente os requisitos RF-01 a RF-15. A implementação é integrada, gratuita para o usuário e offline-first; não se reduz a mockups.

## Coordenação
- O agente principal é responsável por contratos, plano, integração, revisão e relatório. Delegue subagentes independentes quando a sessão do Codex oferecer ferramentas reais de colaboração; mantenha até quatro em paralelo. Se não houver suporte, trabalhe sequencialmente e documente a limitação.
- Especialidades: Android/acessibilidade; backend/push/sincronização; voz/OCR/IA; revisor de usabilidade de idosos; revisor de segurança, privacidade e confiabilidade.
- Defina responsáveis por diretórios antes das edições. Não permita duas alterações concorrentes no mesmo arquivo. Revisores priorizam análise somente leitura e entregam achados ao coordenador.

## Melhoria contínua com fim definido
- Faça duas rodadas: após o primeiro caminho integrado executável e antes da entrega. Cada um dos dois revisores deve apresentar até cinco oportunidades verificáveis em cada rodada.
- Coordenador pode aplicar até três melhorias opcionais por rodada; defeitos críticos de segurança/qualidade são correções obrigatórias, não entram no limite. Todas as mudanças adotadas exigem teste e registro em `docs/IMPROVEMENTS.md`.
- Não expandir escopo indefinidamente. Pare após duas revisões, correções essenciais e testes de aceite. Registre ideias futuras sem implementá-las automaticamente.

## Segurança e qualidade
- Nunca ativar/alterar doses medicamentosas por IA sem revisão humana. Não inferir que silêncio significa dose omitida ou desorientação confirmada.
- Em possível desorientação, orientar familiares a encontrar a pessoa, não induzir retorno desacompanhado.
- Dados reais de saúde, telefones, GPS preciso e segredos não devem aparecer em código, testes, logs ou saídas de agentes. Use dados fictícios.
- Nenhuma dependência de API paga, root, ADB para usuários finais ou permissões proibidas no app distribuível.
- Executar verificações relevantes e indicar explicitamente o que o ambiente não permitiu testar. Nunca alegar que APK, push, geofence, gravação, tradução ou agentes foram verificados sem evidência.

## Arquivos de evidência
- `docs/AGENT_WORK_PLAN.md`: divisão do trabalho, interfaces e estado de integração.
- `docs/IMPROVEMENTS.md`: achados, decisões, duas rodadas e evidências dos testes.
- `IMPLEMENTATION_REPORT.md`: delegações reais, builds, testes, pendências e limitações.
