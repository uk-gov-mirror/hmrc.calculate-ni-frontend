import React, {useState, useContext} from 'react'

// components
import Details from "../shared/Details"
import LateInterestForm from "../late-interest/LateInterestForm"
import LateInterestResults from "../late-interest/LateInterestResults"

// types
import {LateInterestContext, useLateInterestForm} from './LateInterestContext'

const pageTitle = 'Interest on late or unpaid Class 1 NI contributions'

function LateInterestPage() {
  const [showSummary, setShowSummary] = useState<boolean>(false)
  const {
    details,
    setDetails
  } = useContext(LateInterestContext)

  const handleChange = ({
    currentTarget: { name, value },
  }: React.ChangeEvent<HTMLInputElement>) => {
    setDetails({ [name]: value })
  }

  const submitForm = (showSummaryIfValid: boolean) => {

  }

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault()
    submitForm(false)
  }

  return (
    <main>
      {showSummary ?
        <div>Save and print summary</div>
      :
        <>

          {/* error summary*/}
          <div>[Add error summary]</div>

          <h1>{pageTitle}</h1>

          <Details
            details={details}
            handleChange={handleChange}
          />

          <form onSubmit={handleSubmit} noValidate>
            <div className="form-group table-wrapper nomar">
              <LateInterestForm />
            </div>
          </form>

          <LateInterestResults />

        </>
      }
    </main>
  )
}

const LateInterest = () => (
  <LateInterestContext.Provider value={useLateInterestForm()}>
    <LateInterestPage />
  </LateInterestContext.Provider>
)

export default LateInterest