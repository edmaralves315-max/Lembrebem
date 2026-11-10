# Plano e propriedade
Coordenador: contratos, integração, scripts, documentação e evidências.
Android: propriedade exclusiva de android/ (exceto pacote clinical).
Backend: propriedade exclusiva de backend/.
Voz/documentos: propriedade exclusiva de android/app/src/main/java/org/lembrebem/clinical/.
Revisores usabilidade e segurança: somente leitura; relatórios docs/reviews/ de nomes próprios.
Contratos: UUID, instantes UTC ISO-8601, preferredLocale BCP47, countryCode separado, timezone IANA. API /v1; Authorization Bearer Firebase; demonstração explícita. Eventos {id,personId,type,occurredAt,payload}; confirmação preserva horário real. Sem confirmação não significa dose perdida. Android integra módulos clínicos por interfaces locais; nunca publica prescrição automaticamente.
Até quatro subagentes ativos. Revisores iniciam após primeiro caminho executável e repetem rodada B. Sem credenciais reais.

## Consolidação 10/11/2026
Estado inicial preservado no commit54eaf5f e backup tar.gz externo.
Agentes reais anteriores Android/backend/clinical entregaram código; revisores A entregaram relatórios. Retomada android_fixes/backend_fixes corrigiu A. Entrega: coordenador possui workflow/scripts/documentação e correções UI/Sync; usability_b e security_b somente relatórios B; integration_check somente relatório; final_backend_fixes proprietário backend corrigiu B sem conflito Android. Todos sem credenciais reais.
Backend final testado localmente; Android integra clinical em UI e backend por contrato inspecionado, aguardando compilação CI. Worktrees não usados: diretórios exclusivos em workspace inicialmente sem Git; repositório agora inicializado. Nenhum agente afirma APK ou teste físico. Máximo quatro subagentes ativos nesta consolidação.
