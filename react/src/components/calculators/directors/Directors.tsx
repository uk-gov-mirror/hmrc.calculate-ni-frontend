import React, {useContext, useState} from 'react'
import {validateDirectorsPayload} from '../../../validation/validation'
import configuration from '../../../configuration.json'
import {ClassOne} from '../../../calculation'
import {PeriodLabel, PeriodValue, taxYearString} from '../../../config'

// components
import Details from '../../Details'
import DirectorsTable from '../directors/DirectorsTable'
import Totals from '../../Totals'
import ErrorSummary from '../../helpers/gov-design-system/ErrorSummary'
import {updateDirectorsRowInResults} from "../../../services/utils";
import DirectorsPrintView from "./DirectorsPrintView";
import {DirectorsContext, defaultRows} from "./DirectorsContext";

// types
import {
  Calculated,
  DirectorsRow,
  GovDateRange,
} from '../../../interfaces'

const pageTitle = 'Directors’ contributions'

function Directors() {
  const [reset, setReset] = useState<boolean>(false)
  const {
    setDetails,
    rowsErrors,
    setRowsErrors,
    rows,
    taxYear,
    setTaxYear,
    setRows,
    details,
    grossTotal,
    earningsPeriod,
    setEarningsPeriod,
    errors,
    setErrors
  } = useContext(DirectorsContext)

  const [calculatedRows, setCalculatedRows] = useState<Array<Calculated>>([])
  const [showSummary, setShowSummary] = useState<boolean>(false)
  const [showDetails, setShowDetails] = useState(false)
  const [dateRange, setDateRange] = useState<GovDateRange>({from: null, to: null})

  const handleChange = ({
    currentTarget: { name, value },
  }: React.ChangeEvent<HTMLInputElement>) => {
    setDetails({ [name]: value })
  }

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault()
    setErrors({})
    setRowsErrors({})
    const payload = {
      rows: rows,
      niPaidEmployee: '', // not sure this should be in this payload
      niPaidNet: '', // not sure this should be in this payload
      dateRange: dateRange,
      earningsPeriod: earningsPeriod
    }

    if(validateDirectorsPayload(payload, setErrors, setRowsErrors)) {
      setCalculatedRows(
        calculateRows(rows as DirectorsRow[], taxYear.from) as Calculated[]
      )
    }
  }

  const calculateRows = (rows: Array<DirectorsRow>, taxYear: Date) => {
    const classOneCalculator = new ClassOne(JSON.stringify(configuration));

    return rows.map((row: DirectorsRow, index: number) => {
        let calculatedRow: Calculated;
        if (earningsPeriod === PeriodLabel.ANNUAL) {
          calculatedRow = JSON.parse(classOneCalculator
            .calculate(
              taxYear,
              parseFloat(row.gross),
              row.category,
              PeriodValue.ANNUAL,
              1,
              false
            ))
        } else {
          calculatedRow = JSON.parse(classOneCalculator
            .calculateProRata(
              dateRange.from,
              dateRange.to,
              parseFloat(row.gross),
              row.category,
              false
            ))
        }

        setRows(updateDirectorsRowInResults(rows, calculatedRow, index))

        return calculatedRow
      }) as Calculated[]
    }

  const handlePeriodChange = (value: any) => {
    resetTotals()
    setEarningsPeriod(value as PeriodLabel)
  }

  const resetTotals = () => {
    setErrors({})
    setRowsErrors({})
    setRows(defaultRows)
    setCalculatedRows([])
    setReset(true)
  }

  return (
    <main>
      {showSummary ?
        <DirectorsPrintView
          title={pageTitle}
          setShowSummary={setShowSummary}
          details={details}
          taxYearString={taxYearString(taxYear)}
          taxYear={taxYear}
          earningsPeriod={earningsPeriod}
          rows={rows}
          grossTotal={grossTotal}
          calculatedRows={calculatedRows}
          reset={reset}
          setReset={setReset}
        />
        :
        <>
          {(Object.keys(errors).length > 0 || Object.keys(rowsErrors).length > 0) &&
            <ErrorSummary
              errors={errors}
              rowsErrors={rowsErrors}
            />
          }

          <h1>{pageTitle}</h1>
          <div className="clear">
            <h2 className="govuk-heading-m details-heading">Details</h2>
            <button
              type="button"
              className={`toggle icon ${showDetails ? 'arrow-up' : 'arrow-right'}`}
              onClick={() => setShowDetails(!showDetails)}>
                {showDetails ? 'Close details' : 'Open details'}
            </button>
          </div>

          {showDetails &&
            <Details
              details={details}
              handleChange={handleChange}
            />
          }
          <form onSubmit={handleSubmit} noValidate>
            <div className="form-group table-wrapper">
              <DirectorsTable
                errors={errors}
                rowsErrors={rowsErrors}
                resetTotals={resetTotals}
                rows={rows}
                setRows={setRows}
                taxYear={taxYear}
                setTaxYear={setTaxYear}
                setShowSummary={setShowSummary}
                dateRange={dateRange}
                setDateRange={setDateRange}
                handleChange={handleChange}
                earningsPeriod={earningsPeriod}
                handlePeriodChange={handlePeriodChange}
              />
            </div>
          </form>
        </>
      }
      <Totals
        grossPayTally={showSummary}
        errors={errors}
        calculatedRows={calculatedRows}
        isSaveAndPrint={showSummary}
        reset={reset}
        setReset={setReset}
      />
      {showSummary && (
        <div className="govuk-!-padding-bottom-9">
          <button className="button" onClick={() => window.print()}>
            Save and print
          </button>
        </div>
      )}
    </main>
  )
}

export default Directors
