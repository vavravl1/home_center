import React from "react";
import {BootstrapTable, TableHeaderColumn} from "react-bootstrap-table";
import axios from "axios";
import update from "react-addons-update";
import Jumbotron from "react-bootstrap/lib/Jumbotron";

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
                                name: d.name,
                                measuredPhenomenons: d.measuredPhenomenons.map(mp => mp.name + " " + mp.unit).join(),
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

    onCleanData = (address, name) => {
        let t = this;
        let deleteUrl = document.getElementById('bcSensorReading').value +
            address + "/" + name;

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
                data.name
            )}
        >
            Clean data
        </button>
    };

    render = () => {
        return <Jumbotron bsClass="bc-measurement-box">
            <h3>Active sensors</h3>
            <BootstrapTable data={this.state.activeSensors}
                            striped
                            hover
                            selectRow={{mode: 'none'}}
            >
                <TableHeaderColumn isKey dataField='key'
                                   dataFormat={(cell, data, rowIndex) => {
                                       return cell.split(":")[0]
                                   }}>
                    Location</TableHeaderColumn>
                <TableHeaderColumn dataField='name'>Name</TableHeaderColumn>
                <TableHeaderColumn dataField='measuredPhenomenons'>Phenomenons</TableHeaderColumn>
                <TableHeaderColumn
                    dataField='address'
                    dataFormat={this.cleanDataButtonFormatter.bind(this)}
                    hiddenHeader={true}
                />
            </BootstrapTable>
            </Jumbotron>
    }
}

export default ActiveSensorsSettings