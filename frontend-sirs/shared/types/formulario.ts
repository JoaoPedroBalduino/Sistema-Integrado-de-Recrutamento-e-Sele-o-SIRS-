export interface OpcaoFormulario {
  valor: string | number
  rotulo: string
}
export interface CampoFormulario {
  chave: string
  rotulo: string
  tipo?:
    | 'text'
    | 'email'
    | 'password'
    | 'number'
    | 'date'
    | 'datetime-local'
    | 'select'
    | 'textarea'
    | 'checkbox'
    | 'multiple'
  obrigatorio?: boolean
  minimo?: number
  maximo?: number
  passo?: string
  opcoes?: OpcaoFormulario[]
  dica?: string
  valor?: string | number | boolean | number[] | null
}
export type ValoresFormulario = Record<string, string | number | boolean | number[] | null>
