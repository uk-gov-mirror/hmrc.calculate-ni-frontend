import React, {useContext, useState} from 'react';
import uniqid from 'uniqid';
import { taxYearsCategories, taxYearString } from '../../../config'

import numeral from 'numeral'
import 'numeral/locales/en-gb';

import ClassOneEarningsTable from './Class1EarningsTable'

// types
import { Row, Class1TableProps, TaxYear } from '../../../interfaces';
import {ClassOneContext} from "../../../services/ClassOneContext";

numeral.locale('en-gb');

function Class1Table(props: Class1TableProps) {
  const [taxYears] = useState<TaxYear[]>(taxYearsCategories)
  const [activeRowID, setActiveRowID] = useState<string | null>(null)
  const {
    rowsErrors,
    rows,
    taxYear,
    setTaxYear,
    setRows,
  } = useContext(ClassOneContext)
  const handleSetActiveRow = (r: Row) => {
    if (activeRowID !== r.id) setActiveRowID(r.id)
  }

  const handleChange = (r: Row, e: React.ChangeEvent<HTMLInputElement>) => {
    handleSetActiveRow(r)
    setRows(rows.map(
      cur => cur.id === r.id ? {...cur, [`${e.currentTarget.name.split('-')[1]}`]: e.currentTarget.value} : cur
    ))
  }
  
  const handleSelectChange = (r: Row, e: React.ChangeEvent<HTMLSelectElement>) => {
    handleSetActiveRow(r)
    setRows(rows.map(cur =>
      cur.id === r.id ? {...cur, [e.currentTarget.name]: e.currentTarget.value} : cur
    ))
  }

  const handleTaxYearChange = (e: React.ChangeEvent<HTMLSelectElement>) => (
    setTaxYear(taxYears[taxYears.findIndex(ty => ty.id === e.target.value)])
  )

  const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
    e.preventDefault();
    const lastRow = rows[rows.length -1]
    setRows([...rows, {
      id: uniqid(),
      category: lastRow.category,
      period: lastRow.period,
      gross: lastRow.gross,
      number: '',
      ee: '0',
      er: '0'
    }])
  }

  return (
    <div className="form-group table-wrapper">
      <div className="container">
        <div className="form-group half">
          <label className="govuk-label" htmlFor="taxYear">
            Select a tax year
          </label>
          <select value={taxYear.id} onChange={handleTaxYearChange} id="taxYear" name="taxYear" className="govuk-select">
            {taxYears.map((y, i) => (
              <option key={i} value={y.id}>{taxYearString(y)}</option>
            ))}
          </select>
        </div>
      </div>

      <ClassOneEarningsTable
        rows={rows}
        rowsErrors={rowsErrors}
        activeRowID={activeRowID}
        taxYear={taxYear}
        handleChange={handleChange}
        handleSelectChange={handleSelectChange}
        showBands={false}
      />
      
      <div className="container">
        <div className="container">
          <div className="form-group repeat-button">        
            <button 
              className="button govuk-button govuk-button--secondary nomar" 
              onClick={handleClick}>
              Repeat row
            </button>
          </div>

          <div className="form-group">
            <button className="button govuk-button govuk-button--secondary nomar" onClick={(e) => {
              e.preventDefault()
              props.resetTotals()
            }}>
              Clear table
            </button>
          </div>
        </div>
      </div>

      </div>
  )
}

export default Class1Table;