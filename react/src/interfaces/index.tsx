// App
export interface S {
  fullName: string
  ni: string
  reference: string
  preparedBy: string
  date: string
}

export interface Errors {
  niPaidNet: string
  niPaidEmployee: string
}

export interface Row {
  id: string
  category: string
  period: string
  gross: string
  ee: string
  er: string
}

export interface Calculated {
  [key: string]: number[]
}

// Table
export interface TaxYear {
  id: string
  from: Date
  to: Date
  categories: string[]
}

export interface TableProps {
  rows: Row[]
  setRows: (r: Row[]) => void
  runCalcs: (r: Row[], t: Number, ty: Date) => void
  errors: object
  rowsErrors: ErrorSummaryProps['rowsErrors']
  resetTotals: () => void
  periods: string[]
  setTaxYear: (ty: TaxYear) => void
  taxYear: TaxYear
  setShowSummary: (v: Boolean) => void
}

export interface CT {
  rows: Row[]
  rowsErrors?: ErrorSummaryProps['rowsErrors']
  activeRowID?: string | null
  periods: string[]
  taxYear: TaxYear
  handleChange?: (r: Row, e: React.ChangeEvent<HTMLInputElement>) => void
  handleSelectChange?: (r: Row, e: React.ChangeEvent<HTMLSelectElement>) => void
}


// Totals
export interface TotalsProps {
  errors: {
    niPaidNet?: string
    niPaidEmployee?: string
  }
  grossTotal: Number | null
  niPaidNet: string
  niPaidEmployee: string
  niPaidEmployer: number
  netContributionsTotal: number
  employeeContributionsTotal: number
  employerContributionsTotal: number
  underpaymentNet: number
  overpaymentNet: number
  underpaymentEmployee: number
  overpaymentEmployee: number
  underpaymentEmployer: number
  overpaymentEmployer: number
  // handleNiChange: ({ currentTarget: { name, value }, }: React.ChangeEvent<HTMLInputElement>) => void
  setNiPaidNet: (v: string) => void
  setNiPaidEmployee: (v: string) => void
}

// Errors
export interface ErrorSummaryProps {
  errors: {
    niPaidNet?: string
    niPaidEmployee?: string
  }
  rowsErrors: {
    [id: string]: {
      [rowName: string]: {
        link?: string
        message?: string
        name?: string
      }
    }
  }
}

// Save Print
export interface SavePrintProps {
  setShowSummary: (v: Boolean) => void
  details: S
  taxYearString: string
  rows: Row[]
  periods: string[]
  taxYear: TaxYear
}

// Helpers
export interface  SummaryListRowProps {
  listKey: string
  listValue: string
  rowClasses?: String
}