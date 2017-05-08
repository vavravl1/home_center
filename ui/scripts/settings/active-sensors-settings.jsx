import React from "react";
import {BootstrapTable, TableHeaderColumn} from "react-bootstrap-table";
import axios from "axios";
import update from "react-addons-update";

class ActiveSensorsSettings extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            activeSensors: [],
            admin: (document.getElementById('admin').value == 'true')
        };
    };

    componentDidMount = () => {
        this.loadData();
    };

    loadData = () => {
        let t = this;
        axios.get(document.getElementById('bcSensorReading').value)
            .then(function (activeSensors) {
                const newState = update(t.state, {
                    activeSensors: {
                        $set: activeSensors.data.map(d => {
                            let nd = {
                                measuredPhenomenon: d.measuredPhenomenon,
                                address: d.location.address,
                                key: d.location.address + ":" + d.measuredPhenomenon
                            };
                            return nd;
                        })
                    },
                });
                t.setState(newState);
                console.log(JSON.stringify(newState));
            }).catch(function (error) {
            console.log(error);
        });
    };

    onCleanData = (address, measuredPhenomenon) => {
        let t = this;
        let deleteUrl = document.getElementById('bcSensorReading').value +
            address + "/" + measuredPhenomenon;

        axios.delete(deleteUrl).then(function () {
            t.loadData();
        });
    };

    cleanDataButtonFormatter = (cell, data, rowIndex) => {
        return <button
            type="button"
            onClick={this.onCleanData.bind(
                this,
                data.address,
                data.measuredPhenomenon
            )}
        >
            Clean data
        </button>
    };

    render = () => {
        const cellEditProp = {
            mode: 'click',
            blurToSave: true,
            afterSaveCell: this.onAfterSaveLocationCell
        };

        return <BootstrapTable data={this.state.activeSensors}
                               striped
                               hover
                               selectRow={{mode: 'none'}}
        >
            <TableHeaderColumn isKey dataField='key'
                               dataFormat={(cell, data, rowIndex) => {
                                   return cell.split(":")[0]
                               }}>
                Location</TableHeaderColumn>
            <TableHeaderColumn dataField='measuredPhenomenon'>Phenomenon</TableHeaderColumn>
            <TableHeaderColumn
                dataField='address'
                dataFormat={this.cleanDataButtonFormatter.bind(this)}
                hiddenHeader={true}
            />
        </BootstrapTable>
    }
}

export default ActiveSensorsSettings