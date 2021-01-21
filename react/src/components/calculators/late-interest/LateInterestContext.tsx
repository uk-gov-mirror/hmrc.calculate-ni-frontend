import React, {useState} from 'react'
import uniqid from 'uniqid';

// types
import {Class1DebtRow, Class1S, DetailsProps, TaxYear} from '../../../interfaces'
import {buildTaxYears} from "../../../config";
import {ClassOne} from "../../../calculation";
import configuration from "../../../configuration.json";


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
}

export const LateInterestContext = React.createContext<LateInterestContext>(
  {
    details: initialState,
    setDetails: () => {},
    rows: defaultRows,
    setRows: () => {},
    taxYears: taxYears,
  }
)

export function useLateInterestForm() {
  const [details, setDetails] = React.useReducer(stateReducer, initialState)
  const [rows, setRows] = useState<Array<Class1DebtRow>>(defaultRows)

  return {
    details,
    setDetails,
    rows,
    setRows,
    taxYears
  }
}