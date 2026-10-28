export type TipoMensagem = 'informacao' | 'sucesso' | 'erro' | 'alerta'
export interface MensagemSistema {
  identificador: number
  texto: string
  tipo: TipoMensagem
  titulo: string
}
