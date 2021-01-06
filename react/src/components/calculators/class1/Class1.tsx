import React, {useContext, useState} from 'react'
import {validateClassOnePayload} from '../../../validation/validation'
import configuration from '../../../configuration.json'
import { ClassOne } from '../../../calculation'
import {
  taxYearString,
  PeriodValue
} from '../../../config'

// types
import {Row, Calculated} from '../../../interfaces'

// components
import Totals from '../../Totals'
import Class1Print from './Class1Print'
import ErrorSummary from '../../helpers/gov-design-system/ErrorSummary'

// utils
import { updateRowInResults } from "../../../services/utils";
import ClassOneForm from "./ClassOneForm";
import {ClassOneContext, defaultRows} from "./ClassOneContext";
import Class1ResultsTable from "./Class1ResultsTable";
import {useClassOneTotals} from "../../../services/classOneTotals";

const pageTitle = 'Calculate Class 1 National Insurance (NI) contributions'

function Class1() {
  const [calculatedRows, setCalculatedRows] = useState<Array<Calculated>>([])
  const [reset, setReset] = useState<boolean>(false)
  const [showSummary, setShowSummary] = useState<boolean>(false)
  const {
    setDetails,
    errors,
    setErrors,
    rowsErrors,
    setRowsErrors,
    rows,
    taxYear,
    setRows,
    details,
    grossTotal,
    niPaidNet,
    niPaidEmployee,
    setNiPaidEmployee,
    setNiPaidNet
  } = useContext(ClassOneContext)

  const handleDetailsChange = ({
    currentTarget: { name, value },
  }: React.ChangeEvent<HTMLInputElement>) => {
    setDetails({ [name]: value })
  }

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault()
    setRowsErrors({})
    const payload = {
      rows: rows,
      niPaidNet: niPaidNet,
      niPaidEmployee: niPaidEmployee
    }

    if (validateClassOnePayload(payload, setRowsErrors, setErrors)) {
      setCalculatedRows(
        calculateRows(rows as Row[], taxYear.from) as Calculated[]
      )
    }
  }

  const handleEdit = (event: React.FormEvent) => {
    event.preventDefault()
    setErrors({})
    setRowsErrors({})
    setCalculatedRows([])
  }

  const resetTotals = () => {
    setErrors({})
    setRowsErrors({})
    setRows(defaultRows)
    setCalculatedRows([])
    setNiPaidEmployee('')
    setNiPaidNet('')
  }

  const calculateRows = (rows: Row[], taxYear: Date) => {
    const classOneCalculator = new ClassOne(JSON.stringify(configuration));

    return rows
      .map((row, i) => {
        const rowPeriod = (row.period === PeriodValue.FORTNIGHTLY ? PeriodValue.WEEKLY : row.period)
        const rowPeriodQty = (row.period === PeriodValue.FORTNIGHTLY ? 2 : 1)
        const calculatedRow = JSON.parse(
          classOneCalculator
            .calculate(
              taxYear,
              parseFloat(row.gross),
              row.category,
              rowPeriod,
              rowPeriodQty,
              false
            )
        )

        setRows(updateRowInResults(rows, calculatedRow, i))

        return calculatedRow
      }) as Calculated[]
  }

  return (
    <main>
      {calculatedRows?.length > 0 ?
        <>
          {showSummary ?
            <>
              <Class1Print
                title={pageTitle}
                setShowSummary={setShowSummary}
                details={details}
                taxYearString={taxYearString(taxYear)}
                taxYear={taxYear}
                rows={rows}
                grossTotal={grossTotal}
                calculatedRows={calculatedRows}
                reset={reset}
                setReset={setReset}
              />
            </>
            :
            <>
              <h1>{pageTitle}</h1>
              <h2>Calculation results</h2>
              <Class1ResultsTable
                resetTotals={resetTotals}
                setShowSummary={setShowSummary}
              />
            </>
          }
          <Totals
            grossPayTally={showSummary}
            errors={null}
            calculatedRows={calculatedRows}
            isSaveAndPrint={showSummary}
            reset={reset}
            setReset={setReset}
          />
          {showSummary ?
            <div className="govuk-!-padding-bottom-9">
              <button className="button" onClick={() => window.print()}>
                Save and print
              </button>
            </div>
            :
            <div className="container">
              <div className="form-group half">
                <button
                  className="govuk-button govuk-button--secondary"
                  onClick={handleEdit}
                >
                  Change inputs
                </button>
              </div>
              <div className="form-group half">
                <button
                  type="button"
                  className="button govuk-button govuk-button--secondary nomar"
                  onClick={() => setShowSummary(true)}>
                  Save and print
                </button>
              </div>
            </div>
          }
        </>
      :
        <>
          {(Object.keys(rowsErrors).length > 0 || Object.keys(errors).length > 0) &&
            <ErrorSummary
              errors={errors}
              rowsErrors={rowsErrors}
            />
          }
          <h1>{pageTitle}</h1>
          <ClassOneForm
            handleSubmit={handleSubmit}
            handleDetailsChange={handleDetailsChange}
            resetTotals={resetTotals}
          />
        </>
      }
    </main>
  )
}

export default Class1