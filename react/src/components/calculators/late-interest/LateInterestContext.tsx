import React, {Dispatch, SetStateAction, useState} from 'react'
import uniqid from 'uniqid';

// types
import {Class1DebtRow, Class1S, DetailsProps, GovDateRange, TaxYear} from '../../../interfaces'
import {buildTaxYears} from "../../../config";
import {ClassOne} from "../../../calculation";
import configuration from "../../../configuration.json";
import {GenericErrors, LateInterestRowsErrors, RowsErrors} from '../../../validation/validation'


const ClassOneCalculator = new ClassOne(JSON.stringify(configuration))
const taxYears: TaxYear[] = buildTaxYears(ClassOneCalculator.getTaxYears, '')

const initialState = {
  fullName: '',
  ni: '',
  reference: '',
  preparedBy: '',
  date: '',
}

export const defaultRows = [{
  id: uniqid(),
  taxYears: taxYears,
  taxYear: taxYears[0],
  debt: ''
}]

const stateReducer = (state: Class1S, action: { [x: string]: string }) => ({
  ...state,
  ...action,
})

interface LateInterestContext {
  details: DetailsProps
  setDetails: Function
  taxYears: TaxYear[]
  rows: Class1DebtRow[]
  setRows: Function
  dateRange: GovDateRange
  setDateRange: Dispatch<SetStateAction<GovDateRange>>
  errors: GenericErrors
  setErrors: Function
  rowsErrors: LateInterestRowsErrors
  setRowsErrors: Dispatch<LateInterestRowsErrors>
}

export const LateInterestContext = React.createContext<LateInterestContext>(
  {
    details: initialState,
    setDetails: () => {},
    rows: defaultRows,
    setRows: () => {},
    taxYears: taxYears,
    dateRange: {from: null, to: null},
    setDateRange: () => {},
    errors: {},
    setErrors: () => {},
    rowsErrors: {},
    setRowsErrors: () => {}
  }
)

export function useLateInterestForm() {
  const [details, setDetails] = React.useReducer(stateReducer, initialState)
  const [rows, setRows] = useState<Array<Class1DebtRow>>(defaultRows)
  const [dateRange, setDateRange] = useState<GovDateRange>((() => ({from: null, to: null})))
  const [errors, setErrors] = useState<GenericErrors>({})
  const [rowsErrors, setRowsErrors] = useState<LateInterestRowsErrors>({})

  return {
    details,
    setDetails,
    rows,
    setRows,
    taxYears,
    dateRange,
    setDateRange,
    errors,
    setErrors,
    rowsErrors,
    setRowsErrors
  }
}