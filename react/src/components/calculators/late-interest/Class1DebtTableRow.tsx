import React, {useContext} from 'react'

// components
import SelectTaxYear from '../../helpers/formhelpers/SelectTaxYear';
import TextInput from '../../helpers/formhelpers/TextInput'

// types
import {Class1DebtRow, Row, TaxYear} from '../../../interfaces'
import {LateInterestContext} from './LateInterestContext'
import {extractFromDateString, extractToDateString} from '../../../config'

function Class1DebtTableRow(props: {
  row: Class1DebtRow,
  taxYears: TaxYear[]
}) {
  const {row, taxYears} = props
  const {rows, setRows, errors} = useContext(LateInterestContext)

  const handleTaxYearChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const ty = e.currentTarget.value

    const tYObject: TaxYear = {
      id: ty,
      from: new Date(extractFromDateString(ty)),
      to: new Date(extractToDateString(ty))
    }

    setRows(rows.map((cur: Class1DebtRow) =>
      cur.id === row.id ? {...cur, taxYear: tYObject} : cur
    ))
  }

  const handleChange = (row: Class1DebtRow, e:  React.ChangeEvent<HTMLInputElement>) => {
    setRows(rows.map((cur: Class1DebtRow) =>
      cur.id === row.id ? {...cur, debt: e.currentTarget.value} : cur
    ))
  }

  return (
    <tr key={row.id}>
      <td className="input">
        <SelectTaxYear
          borderless={true}
          hiddenLabel={true}
          taxYears={taxYears}
          taxYear={row.taxYear}
          handleTaxYearChange={handleTaxYearChange}
          onlyStartYear={true}
        />
      </td>
      <td className={`input${errors[`${row.id}-class1-debt`] ? ` error-cell` : ``}`}>
        <TextInput
          labelText="Enter Class 1 debt"
          hiddenLabel={true}
          name={`${row.id}-class1-debt`}
          inputClassName="number"
          inputValue={row.debt}
          placeholderText="Enter the Class 1 debt"
          onChangeCallback={(e) => handleChange?.(row, e)}
        />
      </td>
      <td></td>
    </tr>
  )
}

export default Class1DebtTableRow