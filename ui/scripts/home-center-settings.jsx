import React from "react";
import Col from "react-bootstrap/lib/Col";
import axios from "axios";
import {BootstrapTable, TableHeaderColumn} from "react-bootstrap-table";
import update from "react-addons-update";

class HomeCenterSettings extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            bcSensorLocations: [],
            bcActiveSensors: []
        };
    };

    componentDidMount = () => {
        this.loadData();
    };

    loadData = () => {
        let t = this;
        axios.all([
            axios.get(document.getElementById('settingsBackendUrl').value),
            axios.get(document.getElementById('bcSensorReading').value)
        ]).then(axios.spread(function (sensorLocation, activeSensors) {
            const newState = update(t.state, {
                bcSensorLocations: {$set: sensorLocation.data},
                activeSensors: {$set: activeSensors.data},
            });

            t.setState(newState);
            console.log(JSON.stringify(newState));
        })).catch(function (error) {
            console.log(error);
        });
    };

    onAfterSaveCell = (row) => {
        this.postSettings(row.address, row.label);
    };

    onAddRow = (row) => {
        if (row.address.length > 0 && row.label.length > 0) {
            this.postSettings(row.address, row.label);
        }
    };

    onDeleteRow = (row) => {
        let t = this;
        let deleteUrl = document.getElementById('settingsBackendUrl').value + '?location=' + window.btoa(row);

        axios.delete(deleteUrl).then(function () {
            t.loadData();
        });
    };

    postSettings = (address, label) => {
        let t = this;
        let postUrl = document.getElementById('settingsBackendUrl').value;
        axios.post(postUrl, {address: address, label: label})
            .then(function () {
                t.loadData();
            });
    };

    onCleanData = (location, phenomenon) => {
        let t = this;
        let deleteUrl = document.getElementById('bcSensorReading').value +
            location + "/" + phenomenon;

        axios.delete(deleteUrl).then(function () {
            t.loadData();
        });
    };

    buttonFormatter = (cell, data, rowIndex) => {
        console.log(JSON.stringify(cell) + ' ' + JSON.stringify(data) + ' ' + rowIndex);
        return <button
            type="button"
            onClick={this.onCleanData.bind(
                this,
                data.address,
                data.phenomenon
            )}
        >
            Clean data
        </button>
    };

    render = () => {
        const cellEditProp = {
            mode: 'click',
            blurToSave: true,
            afterSaveCell: this.onAfterSaveCell
        };

        return <div>
            <Col xs={6} md={5}>
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
                    <TableHeaderColumn isKey dataField='address'>Address</TableHeaderColumn>
                    <TableHeaderColumn dataField='label'>Label</TableHeaderColumn>
                </BootstrapTable>
            </Col>
            <Col xs={6} md={5}>
                <BootstrapTable data={this.state.activeSensors}
                                striped
                                hover
                                selectRow={{mode: 'radio'}}
                >
                    <TableHeaderColumn isKey dataField='location'>Location</TableHeaderColumn>
                    <TableHeaderColumn dataField='phenomenon'>Phenomenon</TableHeaderColumn>
                    <TableHeaderColumn
                        dataField='location'
                        dataFormat={this.buttonFormatter.bind(this)}
                    />
                </BootstrapTable>
            </Col>
        </div>
    }
}

export default HomeCenterSettings