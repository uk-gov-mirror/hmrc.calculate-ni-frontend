import React, {useContext} from 'react'
import uniqid from 'uniqid'

// components
import Class1DebtTableRow from './Class1DebtTableRow'
import SecondaryButton from '../../helpers/gov-design-system/SecondaryButton'

// types
import {LateInterestContext} from './LateInterestContext'
import {Class1DebtRow} from '../../../interfaces'

function LateInterestDebtTable() {
  const {
    rows,
    setRows,
    taxYears
  } = useContext(LateInterestContext)

  const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
    e.preventDefault()
    const lastRow = rows[rows.length - 1]
    setRows([...rows, {
      id: uniqid(),
      taxYears: taxYears,
      taxYear: taxYears[0],
      debt: ''
    }])
  }

  return (
    <div className="full">
      <h2 className="section-heading">Debt</h2>
      <table className="contribution-details section-outer--top">
        <thead>
          <th><strong>Tax Year</strong></th>
          <th><strong>Class 1 Debt</strong></th>
          <th><strong>Interest Due</strong></th>
        </thead>
        <tbody>
        {rows.map((r: Class1DebtRow, i: number) => (
          <Class1DebtTableRow
            taxYears={taxYears}
            row={r}
            key={r.id}
          />
        ))}
        </tbody>
      </table>

      <div className="container">
        <div className="form-group repeat-button">
          <SecondaryButton
            label="Repeat row"
            onClick={handleClick}
          />
        </div>
      </div>
    </div>
  )
}

export default LateInterestDebtTable