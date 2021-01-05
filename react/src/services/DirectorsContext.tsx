import React, {Dispatch, useEffect, useState} from "react";
import {Class1S, DetailsProps, DirectorsRow, TaxYear} from "../interfaces";
import {PeriodLabel, taxYearsCategories} from "../config";
import {GenericErrors, RowsErrors} from "../validation/validation";
const initialState = {
  fullName: '',
  ni: '',
  reference: '',
  preparedBy: '',
  date: '',
}

export const defaultRows: Array<DirectorsRow> = [{
  id: 'directorsInput',
  category: taxYearsCategories[0].categories[0],
  gross: '',
  ee: '0',
  er: '0'
}]

const stateReducer = (state: Class1S, action: { [x: string]: string }) => ({
  ...state,
  ...action,
})

interface DirectorsContext {
  taxYear: TaxYear
  setTaxYear: Dispatch<TaxYear>
  rows: Array<DirectorsRow>
  setRows: Dispatch<Array<DirectorsRow>>
  details: DetailsProps
  setDetails: Function,
  rowsErrors: RowsErrors,
  setRowsErrors: Dispatch<RowsErrors>
  grossTotal: Number | null,
  setGrossTotal: Dispatch<Number | null>
  earningsPeriod: PeriodLabel | null
  setEarningsPeriod: Dispatch<PeriodLabel | null>
  errors: GenericErrors
  setErrors: Dispatch<GenericErrors>
}

export const DirectorsContext = React.createContext<DirectorsContext>(
  {
    taxYear: taxYearsCategories[0],
    setTaxYear: () => {},
    rows: defaultRows,
    setRows: () => {},
    details: initialState,
    setDetails: () => {},
    rowsErrors: {},
    setRowsErrors: () => {},
    grossTotal: null,
    setGrossTotal: () => {},
    earningsPeriod: null,
    setEarningsPeriod: () => {},
    errors: {},
    setErrors: () => {}
  }
)

export function useDirectorsForm() {
  const [taxYear, setTaxYear] = useState<TaxYear>(taxYearsCategories[0])
  const [rows, setRows] = useState<Array<DirectorsRow>>(defaultRows)
  const [details, setDetails] = React.useReducer(stateReducer, initialState)
  const [grossTotal, setGrossTotal] = useState<Number | null>(null)
  const [errors, setErrors] = useState<GenericErrors>({})
  const [rowsErrors, setRowsErrors] = useState<RowsErrors>({})
  const [earningsPeriod, setEarningsPeriod] = useState<PeriodLabel | null>(null)

  useEffect(() => {
    setGrossTotal(rows.reduce((grossTotal, row) => {
      return grossTotal + parseFloat(row.gross)
    }, 0))
  }, [rows])

  return {
    taxYear,
    setTaxYear,
    rows,
    setRows,
    details,
    setDetails,
    grossTotal,
    setGrossTotal,
    errors,
    setErrors,
    rowsErrors,
    setRowsErrors,
    earningsPeriod,
    setEarningsPeriod
  }
}