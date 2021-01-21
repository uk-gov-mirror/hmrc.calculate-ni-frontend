import React, {useContext} from 'react'

// components
import LateInterestDebtTable from './LateInterestDebtTable'
import LateInterestRemissionPeriods from './LateInterestRemissionPeriods'
import LateInterestRatesTable from './LateInterestRatesTable'
import SecondaryButton from '../../helpers/gov-design-system/SecondaryButton'
import {defaultRows, LateInterestContext} from './LateInterestContext'

function LateInterestForm() {
  const {
    setRows,
    setErrors
  } = useContext(LateInterestContext)

  const handleClearForm = () => {
    // clear form
    setRows(defaultRows)

    // clear results

    // reset errors
    setErrors({})
  }

  return (
    <>
      <div className="container">
        <div className="container two-thirds">
          <LateInterestDebtTable />
        </div>
        <div className="container third">
          <LateInterestRatesTable />
        </div>
      </div>
      <LateInterestRemissionPeriods />

      <div className="container container-block">
        <div className="form-group">
          <button className="govuk-button govuk-!-margin-right-1" type="submit">
            Calculate
          </button>
          <SecondaryButton
            label="Clear"
            onClick={handleClearForm}
          />
        </div>
      </div>
    </>
  )
}

export default LateInterestForm