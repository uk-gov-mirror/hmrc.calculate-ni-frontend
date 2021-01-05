import React, {useContext} from 'react';
import { taxYearString } from '../../../config'

import numeral from 'numeral'
import 'numeral/locales/en-gb';

// types
import {Class1ResultsProps} from '../../../interfaces';
import ClassOneContributionsTable from "./Class1ContributionsTable";
import {ClassOneContext} from "../../../services/ClassOneContext";

numeral.locale('en-gb');

function Class1ResultsTable(props: Class1ResultsProps) {
  const { handleEdit, setShowSummary } = props
  const { taxYear } = useContext(ClassOneContext)
  return (
    <div className="form-group table-wrapper">
      <div className="container">
        <div className="form-group half">
          {taxYearString(taxYear)}
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

      <ClassOneContributionsTable
        showBands={false}
      />
      
      <div className="container">
        <div className="container">
          <div className="form-group">
            <button
              className="govuk-button nomar"
              onClick={handleEdit}
            >
              Edit
            </button>
          </div>
        </div>
      </div>

      </div>
  )
}

export default Class1ResultsTable;