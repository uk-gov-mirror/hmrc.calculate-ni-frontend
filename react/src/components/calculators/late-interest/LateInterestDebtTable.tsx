import React, {useContext} from 'react'

// components
import Class1DebtTableRow from './Class1DebtTableRow'

// types
import {LateInterestContext} from './LateInterestContext'
import {Class1DebtRow} from '../../../interfaces'

function LateInterestDebtTable() {
  const {
    rows,
    taxYears
  } = useContext(LateInterestContext)

  return (
    <table className="contribution-details">
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
  )
}

export default LateInterestDebtTable