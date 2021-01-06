import React, {useContext} from 'react';
import { taxYearString } from '../../../config'

import numeral from 'numeral'
import 'numeral/locales/en-gb';

// types
import {Class1ResultsProps} from '../../../interfaces';
import ClassOneContributionsTable from "./Class1ContributionsTable";
import {ClassOneContext} from "./ClassOneContext";

numeral.locale('en-gb');

function Class1ResultsTable(props: Class1ResultsProps) {
  const { setShowSummary } = props
  const { taxYear } = useContext(ClassOneContext)
  return (
    <div className="form-group table-wrapper">
      <div className="container">
        <div className="form-group half">
          {taxYearString(taxYear)}
        </div>
      </div>
      <ClassOneContributionsTable
        showBands={false}
      />
    </div>
  )
}

export default Class1ResultsTable;