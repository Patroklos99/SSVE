import React from 'react';
import ReactDOM from 'react-dom';
import Trip from './trip';

it('It should mount', () => {
  const div = document.createElement('div');
  ReactDOM.render(<Trip handleEvaluateRes={()=>null}/>, div);
  ReactDOM.unmountComponentAtNode(div);
});