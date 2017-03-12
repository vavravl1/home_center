import React from "react";
import Col from "react-bootstrap/lib/Col";
import FormControl from "react-bootstrap/lib/FormControl";
import Button from "react-bootstrap/lib/Button";
import axios from "axios";
import {BootstrapTable, TableHeaderColumn} from "react-bootstrap-table";
import update from "react-addons-update";

class HomeCenterSettings extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            bcSensorLocations: []
        };
    };

    componentDidMount = () => {
        this.loadData();
    };

    loadData = () => {
        let t = this;
        axios.all([
            axios.get('/settings/bc/sensorLocation'),
        ]).then(axios.spread(function (sensorLocation) {
            const newState = update(t.state, {
                bcSensorLocations: {$set: sensorLocation.data},
            });

            t.setState(newState);
            console.log(JSON.stringify(newState));
        })).catch(function (error) {
            console.log(error);
        });
    };

    onAfterSaveCell = (row) => {
        this.postSettings(row.location, row.label);
    };

    onAddRow = (row) => {
        if (row.location.length > 0 && row.label.length > 0) {
            this.postSettings(row.location, row.label);
        }
    };

    onDeleteRow = (row) => {
        let t = this;
        let csrfTokenName = document.getElementById('csrf_token_name').value;
        let csrfTokenValue = document.getElementById('csrf_token_value').value;
        let deleteUrl = '/settings/bc/sensorLocation/' + row + '?' + csrfTokenName + '=' + csrfTokenValue;

        axios.delete(deleteUrl).then(function () {
            t.loadData();
        });
    };

    postSettings = (location, label) => {
        let t = this;
        let csrfTokenName = document.getElementById('csrf_token_name').value;
        let csrfTokenValue = document.getElementById('csrf_token_value').value;

        let postUrl = '/settings/bc/sensorLocation' + '?' + csrfTokenName + '=' + csrfTokenValue;
        axios.post(postUrl, {location: location, label: label})
            .then(function () {
                t.loadData();
            });
    };

    render = () => {
        const cellEditProp = {
            mode: 'click',
            blurToSave: true,
            afterSaveCell: this.onAfterSaveCell
        };

        return <Col xs={6} md={5}>
            <BootstrapTable data={this.state.bcSensorLocations}
                            cellEdit={cellEditProp}
                            striped
                            hover
                            deleteRow
                            insertRow
                            selectRow={{mode: 'radio'}}
                            options={{
                                onDeleteRow: this.onDeleteRow,
                                onAddRow: this.onAddRow
                            }}
            >
                <TableHeaderColumn isKey dataField='location'>Location</TableHeaderColumn>
                <TableHeaderColumn dataField='label'>Label</TableHeaderColumn>
            </BootstrapTable>
        </Col>
    }
}

export default HomeCenterSettings