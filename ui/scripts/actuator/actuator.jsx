import React from "react";
import axios from "axios";
import update from "react-addons-update";
import {BootstrapTable, TableHeaderColumn} from "react-bootstrap-table";
import Jumbotron from "react-bootstrap/lib/Jumbotron";
import Col from "react-bootstrap/lib/Col";
import FormGroup from "react-bootstrap/lib/FormGroup";
import FormControl from "react-bootstrap/lib/FormControl";
import ControlLabel from "react-bootstrap/lib/ControlLabel";

class Actuator extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            actuators: []
        }
    };

    componentDidMount = () => {
        this.loadData(this);
    };

    loadData = () => {
        let t = this;
        axios.get(document.getElementById('actuatorsUrl').value)
            .then(function (actuators) {
                const newState = update(t.state, {
                    actuators: {
                        $set: actuators.data
                    },
                });
                t.setState(newState);
            }).catch(function (error) {
            console.log(error);
        });
    };

    componentWillUnmount = () => {
    };

    requiredArgumentsRenderer = (cell, data, rowIndex) => {
        if(data.requiredArguments.length == 0) {
            return <button
                type="button"
                // onClick={this.onCleanData.bind(
                //     this,
                //     data.address,
                //     data.name
                // )}
            >
                {data.name}
            </button>
        } else {
            return <form>
                <FormGroup>
                    <ControlLabel>{data.requiredArguments[0].name} [{data.requiredArguments[0].unit}]</ControlLabel>
                    <FormControl placeholder={data.requiredArguments[0].value} />
                </FormGroup>
            </form>
        }
    };

    actuatorView = (actuator) => {
        return <Jumbotron bsClass="bc-measurement-box">
            <h3>{actuator.name}</h3>
            <BootstrapTable data={actuator.supportedCommands}
                            striped
                            hover
                            selectRow={{mode: 'none'}}
            >
                <TableHeaderColumn isKey dataField='name'>Name</TableHeaderColumn>
                <TableHeaderColumn
                    dataField='requiredArguments'
                    dataFormat={this.requiredArgumentsRenderer.bind(this)}
                    hiddenHeader={true}
                />
            </BootstrapTable>
        </Jumbotron>

    };

    render = () => {
        return <div>
            <Col xs={6} md={5}>
                {this.state.actuators.map(actuator => this.actuatorView(actuator))}
            </Col>
        </div>
    };
}

Actuator.propTypes = {};


export default Actuator;